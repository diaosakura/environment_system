package edu.net.environment.controller;

import edu.net.environment.model.*;
import edu.net.environment.model.Requests.*;
import edu.net.environment.service.EnvironmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class EnvironmentController {
    private final EnvironmentService service;
    public EnvironmentController(EnvironmentService service) { this.service=service; }
    private static SessionUser user(HttpSession session) { return (SessionUser)session.getAttribute("user"); }
    @GetMapping("/health") public ApiResult<?> health() { service.ping(); return ApiResult.ok(Map.of("status","UP","database","MySQL")); }
    @PostMapping("/auth/login") public ApiResult<?> login(@Valid @RequestBody Login body,HttpServletRequest req) {
        var result=service.login(body);
        var previous=req.getSession(false);
        if (previous!=null) previous.invalidate();
        req.getSession(true).setAttribute("user",result);
        return ApiResult.ok(result);
    }
    @PostMapping("/auth/register") public ApiResult<?> register(@Valid @RequestBody Register body) {
        service.register(body); return ApiResult.ok(null);
    }
    @GetMapping("/auth/me") public ApiResult<?> me(HttpSession session) { return ApiResult.ok(user(session)); }
    @PostMapping("/auth/logout") public ApiResult<?> logout(HttpSession session) { session.invalidate(); return ApiResult.ok(null); }
    @GetMapping("/dictionaries/provinces") public ApiResult<?> provinces() { return ApiResult.ok(service.provinces()); }
    @GetMapping("/dictionaries/cities") public ApiResult<?> cities(@RequestParam int provinceId) { return ApiResult.ok(service.cities(provinceId)); }
    @GetMapping("/dictionaries/aqi-levels") public ApiResult<?> levels() { return ApiResult.ok(service.levels()); }
    @PostMapping("/supervisor/feedbacks") public ApiResult<?> feedback(@Valid @RequestBody Feedback body,HttpSession session) {
        return ApiResult.ok(service.createFeedback(user(session),body));
    }
    @GetMapping({"/supervisor/feedbacks","/grid/tasks","/admin/feedbacks"})
    public ApiResult<?> feedbacks(@RequestParam Map<String,String> params,HttpSession session) {
        return ApiResult.ok(service.feedbacks(user(session),params));
    }
    @GetMapping({"/supervisor/feedbacks/{id}","/grid/tasks/{id}","/admin/feedbacks/{id}"})
    public ApiResult<?> detail(@PathVariable long id,HttpSession session) { return ApiResult.ok(service.detail(user(session),id)); }
    @PostMapping("/admin/feedbacks/{id}/assignment")
    public ApiResult<?> assign(@PathVariable long id,@Valid @RequestBody Assignment body,HttpSession session) {
        service.assign(user(session),id,body); return ApiResult.ok(null);
    }
    @PostMapping("/grid/tasks/{id}/measurement")
    public ApiResult<?> measure(@PathVariable long id,@Valid @RequestBody Measurement body,HttpSession session) {
        return ApiResult.ok(service.measure(user(session),id,body));
    }
    @GetMapping("/admin/grid-members")
    public ApiResult<?> grids(@RequestParam(required=false) Integer cityId,@RequestParam(required=false) String keyword) {
        return ApiResult.ok(service.gridMembers(cityId,keyword));
    }
    @PostMapping("/admin/grid-members")
    public ApiResult<?> createGrid(@Valid @RequestBody GridMember body,HttpSession session) {
        return ApiResult.ok(service.createGrid(user(session),body));
    }
    @PutMapping("/admin/grid-members/{id}")
    public ApiResult<?> updateGrid(@PathVariable long id,@Valid @RequestBody GridMember body,HttpSession session) {
        service.updateGrid(user(session),id,body); return ApiResult.ok(null);
    }
    @GetMapping("/admin/supervisors")
    public ApiResult<?> supervisors(@RequestParam(required=false) String keyword) { return ApiResult.ok(service.supervisors(keyword)); }
    @PutMapping("/admin/supervisors/{id}")
    public ApiResult<?> updateSupervisor(@PathVariable String id,@Valid @RequestBody Supervisor body,HttpSession session) {
        service.updateSupervisor(user(session),id,body); return ApiResult.ok(null);
    }
    @PutMapping("/admin/grid-members/{id}/password")
    public ApiResult<?> resetGrid(@PathVariable String id,@Valid @RequestBody Password body,HttpSession session) {
        service.resetGridPassword(user(session),id,body); return ApiResult.ok(null);
    }
    @PutMapping("/admin/supervisors/{id}/password")
    public ApiResult<?> resetSupervisor(@PathVariable String id,@Valid @RequestBody Password body,HttpSession session) {
        service.resetSupervisorPassword(user(session),id,body); return ApiResult.ok(null);
    }
    @GetMapping("/admin/operation-logs")
    public ApiResult<?> logs(@RequestParam(defaultValue="1") String page,@RequestParam(defaultValue="10") String size) {
        return ApiResult.ok(service.logs(EnvironmentService.positive(page,1,1000000),EnvironmentService.positive(size,10,100)));
    }
    @GetMapping({"/admin/statistics","/screen/statistics"})
    public ApiResult<?> statistics() { return ApiResult.ok(service.statistics()); }
}
