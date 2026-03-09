package com.ray.atten.middle.module.controller;

import com.ray.atten.middle.module.aspect.LogOperation;
import com.ray.atten.middle.module.dto.CardTemplateRequest;
import com.ray.atten.middle.module.dto.GlobalResponseBody;
import com.ray.atten.middle.module.dto.PrintPayloadDto;
import com.ray.atten.middle.module.dto.PrintPayloadRequest;
import com.ray.atten.middle.module.service.CardTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/api/card-template")
@RequiredArgsConstructor
@Validated
public class CardTemplateController {

    @Autowired
    private CardTemplateService cardTemplateService;

    // 临时缓存：Key是UUID，Value是打印请求数据
    private final Map<String, PrintPayloadRequest> printCache = new ConcurrentHashMap<>();

    /**
     * 1. 接收长参数，存入缓存，返回短令牌
     */
    @LogOperation("获取打印令牌")
    @PostMapping("/prepare-print")
    @ResponseBody
    public ResponseEntity<String> preparePrint(@RequestBody PrintPayloadRequest request) {
        String ticket = UUID.randomUUID().toString().replace("-", "");
        printCache.put(ticket, request);

        // 自动清理：5分钟后删除，防止内存溢出
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1800000);
            } catch (InterruptedException ignored) {
            }
            printCache.remove(ticket);
        });

        return ResponseEntity.ok(ticket);
    }

    /**
     * 跳转到打印页面
     */
    @LogOperation("跳转打印页面")
    @GetMapping("/go-print/{ticket}")
    public String goPrint(@PathVariable String ticket) {
        return "redirect:/badge_print.html?ticket=" + ticket;
    }

    /**
     * 前端根据令牌取数据
     */
    @LogOperation("根据令牌获取数据")
    @GetMapping("/payload-by-ticket/{ticket}")
    @ResponseBody
    public ResponseEntity<GlobalResponseBody> getPayloadByTicket(@PathVariable String ticket) {
        try {
            PrintPayloadRequest request = printCache.get(ticket);
            if (request == null) {
                throw new RuntimeException("打印链接已过期或无效");
            }
            PrintPayloadDto printPayload = cardTemplateService.getPrintPayload(
                    request.getTemplateId(),
                    request.getEmployeeIds()
            );
            return ResponseEntity.ok(new GlobalResponseBody("200", "SUCCESS", printPayload));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(new GlobalResponseBody("500", "ERROR", e.getMessage()));
        }
    }

    /**
     * 保存或更新模板
     */
    @LogOperation("保存或更新模板")
    @PostMapping("/save")
    public ResponseEntity<GlobalResponseBody> saveTemplate(@RequestBody @Valid CardTemplateRequest request) {
        try {
            // 这里可以直接将 request 传入 Service 处理逻辑
            return ResponseEntity.ok(new GlobalResponseBody("200", "SUCCESS", cardTemplateService.saveTemplate(request)));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(new GlobalResponseBody("500", "ERROR", "保存失败"));
        }
    }

    /**
     * 获取所有激活的模板列表 (供下拉菜单选择)
     */
    @GetMapping("/list-active")
    public ResponseEntity<GlobalResponseBody> listActive() {
        try {
            return ResponseEntity.ok(new GlobalResponseBody("200", "SUCCESS", cardTemplateService.findAllActive()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(new GlobalResponseBody("500", "ERROR", "获取列表失败"));
        }
    }

    /**
     * 删除模板
     */
    @LogOperation("删除模板")
    @DeleteMapping("/{id}")
    public ResponseEntity<GlobalResponseBody> deleteTemplate(@PathVariable UUID uuid) {
        try {
            cardTemplateService.deleteTemplateByUuid(uuid);
            return ResponseEntity.ok(new GlobalResponseBody("200", "SUCCESS", "删除模板成功"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(new GlobalResponseBody("500", "ERROR", "删除模板失败"));
        }
    }

    /**
     * 切换模板状态
     */
    @LogOperation("切换模板状态")
    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<GlobalResponseBody> toggleActive(@PathVariable UUID uuid) {
        try {
            cardTemplateService.toggleActiveByUuid(uuid);
            return ResponseEntity.ok(new GlobalResponseBody("200", "SUCCESS", "修改模板状态成功"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(new GlobalResponseBody("500", "ERROR", "修改模板状态失败"));
        }
    }


}
