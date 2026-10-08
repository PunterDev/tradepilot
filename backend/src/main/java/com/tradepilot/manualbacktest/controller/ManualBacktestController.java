package com.tradepilot.manualbacktest.controller;

import com.tradepilot.entity.User;
import com.tradepilot.manualbacktest.dto.ManualBacktestDtos.*;
import com.tradepilot.manualbacktest.service.ManualBacktestService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping({"/api/manual-backtests", "/api/v1/manual-backtests"})
public class ManualBacktestController {
    private final ManualBacktestService service;
    public ManualBacktestController(ManualBacktestService service){this.service=service;}
    @GetMapping("/access") public Access access(@AuthenticationPrincipal User user){return service.access(user);}
    @GetMapping("/sessions") public List<Session> list(@AuthenticationPrincipal User user){return service.list(user);}
    @PostMapping("/sessions") public Session create(@AuthenticationPrincipal User user,@RequestBody CreateSession request){return service.create(user,request);}
    @GetMapping("/sessions/{id}") public Session get(@AuthenticationPrincipal User user,@PathVariable Long id){return service.get(user,id);}
    @PostMapping("/sessions/{id}/advance") public Session advance(@AuthenticationPrincipal User user,@PathVariable Long id,@RequestBody Advance request){return service.advance(user,id,request);}
    @GetMapping("/sessions/{id}/chart") public Chart chart(@AuthenticationPrincipal User user,@PathVariable Long id,@RequestParam(defaultValue="1m") String timeframe){return service.chart(user,id,timeframe);}
    @PostMapping("/sessions/{id}/orders") public Trade order(@AuthenticationPrincipal User user,@PathVariable Long id,@RequestBody Order request){return service.placeOrder(user,id,request);}
    @PatchMapping("/sessions/{id}/position") public Trade position(@AuthenticationPrincipal User user,@PathVariable Long id,@RequestBody PositionChange request){return service.updatePosition(user,id,request);}
    @PostMapping("/sessions/{id}/order/cancel") public Trade cancelOrder(@AuthenticationPrincipal User user,@PathVariable Long id){return service.cancelPendingOrder(user,id);}
    @PostMapping("/sessions/{id}/position/break-even") public Trade breakEven(@AuthenticationPrincipal User user,@PathVariable Long id){return service.breakEven(user,id);}
    @PostMapping("/sessions/{id}/position/flatten") public Trade flatten(@AuthenticationPrincipal User user,@PathVariable Long id){return service.flatten(user,id);}
    @PostMapping("/sessions/{id}/complete") public Session complete(@AuthenticationPrincipal User user,@PathVariable Long id){return service.complete(user,id);}
}
