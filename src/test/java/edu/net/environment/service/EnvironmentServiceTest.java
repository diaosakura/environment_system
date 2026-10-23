package edu.net.environment.service;

import edu.net.environment.mapper.EnvironmentMapper;
import edu.net.environment.model.*;
import edu.net.environment.model.Requests.*;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class EnvironmentServiceTest {
    EnvironmentMapper mapper;
    EnvironmentService service;
    SessionUser grid=new SessionUser("1","网格员","grid"),admin=new SessionUser("1","管理员","admin");
    Measurement measurement=new Measurement(BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ONE);
    @BeforeEach void setup() { mapper=mock(EnvironmentMapper.class); service=new EnvironmentService(mapper,new AqiCalculator()); }
    @Test void otherGridMemberCannotConfirm() {
        when(mapper.lockFeedback(10)).thenReturn(Map.of("state",1,"gridMemberId",2L));
        assertEquals(403,assertThrows(ApiException.class,()->service.measure(grid,10,measurement)).status());
        verify(mapper,never()).levels();
    }
    @Test void completedTaskCannotConfirmAgain() {
        when(mapper.lockFeedback(10)).thenReturn(Map.of("state",2,"gridMemberId",1L));
        assertEquals(409,assertThrows(ApiException.class,()->service.measure(grid,10,measurement)).status());
        verify(mapper,never()).levels();
    }
    @Test void assignmentMustUseSameCity() {
        when(mapper.lockFeedback(10)).thenReturn(Map.of("state",0,"cityId",6));
        when(mapper.lockGrid(2)).thenReturn(Map.of("enabled",true,"cityId",17));
        assertThrows(ApiException.class,()->service.assign(admin,10,new Assignment(2L)));
        verify(mapper,never()).assign(anyLong(),anyLong());
    }
    @Test void existingAssignmentCannotBeOverwritten() {
        when(mapper.lockFeedback(10)).thenReturn(Map.of("state",1,"cityId",6));
        assertEquals(409,assertThrows(ApiException.class,()->service.assign(admin,10,new Assignment(2L))).status());
        verify(mapper,never()).lockGrid(anyLong());
    }
    @Test void supervisorCannotReadAnotherPersonsFeedback() {
        when(mapper.feedback(1)).thenReturn(Map.of("supervisorId","13800000002"));
        assertEquals(403,assertThrows(ApiException.class,()->service.detail(new SessionUser("13800000001","甲","supervisor"),1)).status());
    }
    @Test void gridMemberWithPendingTasksCannotBeDisabledOrMoved() {
        when(mapper.lockGrid(1)).thenReturn(Map.of("cityId",6));
        when(mapper.validCity(6,6)).thenReturn(1);
        when(mapper.pendingTasks(1)).thenReturn(2L);
        assertEquals(409,assertThrows(ApiException.class,()->service.updateGrid(admin,1,new GridMember("grid1","甲","13900000001",6,6,false))).status());
        verify(mapper,never()).updateGrid(anyLong(),any());
    }
    @Test void invalidPaginationIsRejected() {
        assertThrows(ApiException.class,()->EnvironmentService.positive("0",10,100));
        assertThrows(ApiException.class,()->EnvironmentService.positive("101",10,100));
        assertThrows(ApiException.class,()->EnvironmentService.positive("abc",10,100));
    }
}
