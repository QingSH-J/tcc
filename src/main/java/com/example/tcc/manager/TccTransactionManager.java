package com.example.tcc.manager;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.tcc.context.Participant;
import com.example.tcc.context.TccTransactionContextHolder;
import com.example.tcc.entity.TccLog;
import com.example.tcc.repository.TccLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TccTransactionManager {
    @Autowired 
    private TccLogRepository tccLogRepository;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ApplicationContext applicationContext;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * begin a tcc transaction
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void begin(){
        String txid = UUID.randomUUID().toString();
        TccTransactionContextHolder.setContext(txid);

        TccLog tccLog = new TccLog();
        tccLog.setTxId(txid);
        tccLog.setStatus("TRYING");
        tccLog.setCreatedTime(LocalDateTime.now());
        tccLogRepository.save(tccLog);
        
        log.info("TCC Transaction started with ID: {}", txid);
    }

    /**
     * Register participant
     * @param participant
     */
    @SneakyThrows
    public void registerParticipant(Participant participant){
        String txId = TccTransactionContextHolder.getContext();
        if(txId == null) {
            log.warn("No transaction context found when registering participant");
            return;
        }
        
        String redisKey = "tcc:tx:" + txId + ":participants";
        String participantJson = objectMapper.writeValueAsString(participant);
        redisTemplate.opsForList().rightPush(redisKey, participantJson);
        
        log.info("Registered participant: {} for transaction: {}", participant.getBeanName(), txId);
    }

    @SneakyThrows
    @Transactional(propagation = Propagation.REQUIRES_NEW)  // Add transaction annotation
    public void commit(){
        String txId = TccTransactionContextHolder.getContext();
        if(txId == null){
            log.warn("No transaction context found during commit");
            return;
        }
        
        log.info("Starting commit for transaction: {}", txId);
        
        try {
            TccLog tccLog = tccLogRepository.findById(txId).orElse(null);
            //check if the status is TRYING or tccLog is null
            if(tccLog == null){
                log.error("TccLog not found for transaction: {}", txId);
                return;
            }
            
            if(!"TRYING".equals(tccLog.getStatus())){
                log.warn("Transaction {} is not in TRYING status, current status: {}", txId, tccLog.getStatus());
                return;
            }
            
            //update status to CONFIRMING
            tccLog.setStatus("CONFIRMING");
            tccLogRepository.save(tccLog);
            log.info("Updated transaction {} status to CONFIRMING", txId);

            //from redis get all participants
            String redisKey = "tcc:tx:" + txId + ":participants";
            List<Participant> participants = getParticipants(redisKey);
            
            log.info("Found {} participants for transaction {}", participants.size(), txId);
            
            // Execute confirm for each participant
            for(Participant p : participants){
                try {
                    log.info("Executing confirm for participant: {}, method: {}", p.getBeanName(), p.getConfirmMethod());
                    Object bean = applicationContext.getBean(p.getBeanName());
                    Method method = findMethod(bean.getClass(), p.getConfirmMethod(), p.getArgs());
                    method.invoke(bean, p.getArgs());
                    log.info("Successfully confirmed participant: {}", p.getBeanName());
                } catch (Exception e) {
                    log.error("Error confirming participant: {}", p.getBeanName(), e);
                    throw e;
                }
            }
            
            // Clean up after successful commit
            tccLogRepository.deleteById(txId);
            redisTemplate.delete(redisKey);
            
            log.info("Successfully committed transaction: {}", txId);
        } 
        catch(Exception e) {
            log.error("Error during commit for transaction: {}", txId, e);
            throw e;
        }
        finally{
            TccTransactionContextHolder.clearContext();
        }
    }

    @SneakyThrows
    @Transactional(propagation = Propagation.REQUIRES_NEW)  // Add transaction annotation
    public void rollback() {
        String txId = TccTransactionContextHolder.getContext();
        if (txId == null) {
            log.warn("No transaction context found during rollback");
            return;
        }
        
        log.info("Starting rollback for transaction: {}", txId);
        
        try {
            TccLog tccLog = tccLogRepository.findById(txId).orElse(null);
            if (tccLog == null) {
                log.error("TccLog not found for transaction: {}", txId);
                return;
            }
            
            if (!"TRYING".equals(tccLog.getStatus())) {
                log.warn("Transaction {} is not in TRYING status, current status: {}", txId, tccLog.getStatus());
                return;
            }
            
            tccLog.setStatus("CANCELING");
            tccLogRepository.save(tccLog);
            log.info("Updated transaction {} status to CANCELING", txId);
            
            String redisKey = "tcc:tx:" + txId + ":participants";
            List<Participant> participants = getParticipants(redisKey);
            
            log.info("Found {} participants to rollback for transaction {}", participants.size(), txId);
            
            // Rollback in reverse order
            for (int i = participants.size() - 1; i >= 0; i--) {
                Participant p = participants.get(i);
                try {
                    log.info("Executing cancel for participant: {}, method: {}", p.getBeanName(), p.getCancelMethod());
                    Object bean = applicationContext.getBean(p.getBeanName());
                    Method method = findMethod(bean.getClass(), p.getCancelMethod(), p.getArgs());
                    method.invoke(bean, p.getArgs());
                    log.info("Successfully cancelled participant: {}", p.getBeanName());
                } catch (Exception e) {
                    log.error("Error cancelling participant: {}", p.getBeanName(), e);
                    // Continue with other participants even if one fails
                }
            }
            
            // Clean up after rollback
            tccLogRepository.deleteById(txId);
            redisTemplate.delete(redisKey);
            
            log.info("Successfully rolled back transaction: {}", txId);
        } finally {
            TccTransactionContextHolder.clearContext();
        }
    }
    
    @SneakyThrows
    private List<Participant> getParticipants(String redisKey){
        List<String> participantsJson = redisTemplate.opsForList().range(redisKey, 0, -1);
        if(participantsJson == null || participantsJson.isEmpty()){
            log.warn("No participants found for key: {}", redisKey);
            return List.of();
        }
        return participantsJson.stream().map(this::deserializeParticipant).collect(Collectors.toList());
    }

    @SneakyThrows
    private Participant deserializeParticipant(String participantJson) {
        return objectMapper.readValue(participantJson, Participant.class);
    }

    @SneakyThrows
    private Method findMethod(Class<?> clazz, String methodName, Object[] args) {
        log.debug("Looking for method {} in class {} with {} arguments", methodName, clazz.getName(), args.length);
        
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == args.length) {
                Class<?>[] methodParamTypes = method.getParameterTypes();
                boolean isMatch = true;
                
                for (int i = 0; i < args.length; i++) {
                    Object arg = args[i];
                    Class<?> methodParamType = methodParamTypes[i];
                    
                    if (arg == null) {
                        continue;
                    }
                    
                    if (!methodParamType.isAssignableFrom(arg.getClass()) && 
                        !isWrapperTypeOf(methodParamType, arg.getClass())) {
                        isMatch = false;
                        break;
                    }
                }
                
                if (isMatch) {
                    log.debug("Found method {} in class {}", methodName, clazz.getName());
                    return method;
                }
            }
        }
        
        throw new NoSuchMethodException("In class " + clazz.getName() + " don't find method '" + methodName + "' compatible with args");
    }
    
    private boolean isWrapperTypeOf(Class<?> primitive, Class<?> wrapper) {
        if (!primitive.isPrimitive()) {
            return false;
        }
        try {
            return wrapper.getField("TYPE").get(null).equals(primitive);
        } catch (Exception e) {
            return false;
        }
    }
}