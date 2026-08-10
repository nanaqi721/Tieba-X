package com.mint.ai.contrtroller;

import com.mint.ai.common.Result;
import com.mint.ai.common.dto.CreateBarRequest;
import com.mint.ai.service.BarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 吧控制层
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/api/bars")
public class BarController {


    private final BarService barService;

    @PostMapping("/v1/create")
    public Result<Long> createBar(@Valid @RequestBody CreateBarRequest request){
        Long l = barService.createBar(request);
    }
}
