package edu.net.environment.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import edu.net.environment.mapper.EnvironmentMapper;
import edu.net.environment.model.SessionUser;
import java.util.Map;

@Aspect
@Component
@Order(1)
@EnableTransactionManagement(order=0)
public class AuditAspect {
    private final EnvironmentMapper mapper;
    public AuditAspect(EnvironmentMapper mapper) { this.mapper=mapper; }
    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint point,Audited audited) throws Throwable {
        Object result=point.proceed();
        SessionUser actor=null;
        String target="-";
        for (Object arg:point.getArgs()) {
            if (arg instanceof SessionUser user) actor=user;
            else if (arg instanceof Long || arg instanceof String) target=arg.toString();
        }
        if (result instanceof SessionUser user) { actor=user; target=user.id(); }
        if (result instanceof Map<?,?> map && map.get("id") != null) target=map.get("id").toString();
        if (actor == null) throw new IllegalStateException("Audited write has no actor");
        mapper.log(actor.id(),actor.name(),actor.role(),audited.value(),target);
        return result;
    }
}
