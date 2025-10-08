package com.example.tcc.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.example.tcc.anno.TccAction;
import com.example.tcc.context.Participant;
import com.example.tcc.context.TccTransactionContextHolder;
import com.example.tcc.manager.TccTransactionManager;

@Aspect
@Component
public class TccAop {
    
    @Autowired
    private TccTransactionManager tccTranscationManager;
    
    @Around("@annotation(com.example.tcc.anno.TccGlobalTransaction)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            tccTranscationManager.begin();
            Object result = joinPoint.proceed();
            tccTranscationManager.commit();
            return result;
        } catch (Exception e) {
            tccTranscationManager.rollback();
            throw e;
        }
    }

    @Around("@annotation(tccAction)")
    public Object tccActionAround(ProceedingJoinPoint joinPoint, TccAction tccAction) throws Throwable{
        String txId = TccTransactionContextHolder.getContext();
        if (txId == null) {
            return joinPoint.proceed();
        } 
        String beanName = StringUtils.uncapitalize(joinPoint.getTarget().getClass().getSimpleName());
        Participant participant = new Participant(
            beanName,
            tccAction.confirmMethod(),
            tccAction.cancelMethod(),
            joinPoint.getArgs()
        );
        tccTranscationManager.registerParticipant(participant);
        return joinPoint.proceed();
    }
}
