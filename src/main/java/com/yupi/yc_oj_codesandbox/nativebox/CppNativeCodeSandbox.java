package com.yupi.yc_oj_codesandbox.nativebox;

import cn.hutool.core.io.resource.ResourceUtil;
import com.yupi.yc_oj_codesandbox.model.ExecuteCodeRequest;
import com.yupi.yc_oj_codesandbox.model.ExecuteCodeResponse;
import com.yupi.yc_oj_codesandbox.templatebox.CppCodeSandboxTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;


/**
 * Cpp 原生代码沙箱实现（直接复用模板方法）
 */
@Component
public  class CppNativeCodeSandbox extends CppCodeSandboxTemplate {

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }


    public static void main(String[] args) {
        CppNativeCodeSandbox cppNativeCodeSandbox = new CppNativeCodeSandbox();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "1 3"));
        String code = ResourceUtil.readStr("testCode/cpp/Main.cpp", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("cpp");
        ExecuteCodeResponse executeCodeResponse = cppNativeCodeSandbox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }


}
