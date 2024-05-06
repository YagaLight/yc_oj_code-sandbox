package com.yupi.yc_oj_codesandbox.controller;


import com.yupi.yc_oj_codesandbox.nativebox.CNativeCodeSandbox;
import com.yupi.yc_oj_codesandbox.nativebox.CppNativeCodeSandbox;
import com.yupi.yc_oj_codesandbox.nativebox.JavaNativeCodeSandbox;
import com.yupi.yc_oj_codesandbox.model.ExecuteCodeRequest;
import com.yupi.yc_oj_codesandbox.model.ExecuteCodeResponse;
import com.yupi.yc_oj_codesandbox.nativebox.PythonNativeCodeSandbox;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController("/")
public class MainController {

    // 定义鉴权请求头和密钥
    private static final String AUTH_REQUEST_HEADER = "auth";

    private static final String AUTH_REQUEST_SECRET = "secretKey";

    @Resource
    private JavaNativeCodeSandbox javaNativeCodeSandbox;
    @Resource
    private CNativeCodeSandbox cNativeCodeSandbox;
    @Resource
    private CppNativeCodeSandbox cppNativeCodeSandbox;
    @Resource
    private PythonNativeCodeSandbox pythonNativeCodeSandbox;

    @GetMapping("/health")
    public String healthCheck() {
        return "ok";
    }

    /**
     * 执行代码
     *
     * @param executeCodeRequest
     * @return
     */
    @PostMapping("/executeCode")
    ExecuteCodeResponse executeCode(@RequestBody ExecuteCodeRequest executeCodeRequest, HttpServletRequest request,
                                    HttpServletResponse response) {
        // 基本的认证
        String authHeader = request.getHeader(AUTH_REQUEST_HEADER);
        if (!AUTH_REQUEST_SECRET.equals(authHeader)) {
            response.setStatus(403);
            return null;
        }
        if (executeCodeRequest == null) {
            throw new RuntimeException("请求参数为空");
        }

        switch (executeCodeRequest.getLanguage()){
            case "java":
                return javaNativeCodeSandbox.executeCode(executeCodeRequest);
            case "c":
                return cNativeCodeSandbox.executeCode(executeCodeRequest);
            case "cpp":
                return cppNativeCodeSandbox.executeCode(executeCodeRequest);
            case "python":
                return pythonNativeCodeSandbox.executeCode(executeCodeRequest);
        }

        return null;
    }
}
