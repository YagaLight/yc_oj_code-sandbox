package com.yupi.yc_oj_codesandbox.nativebox;

import cn.hutool.core.io.resource.ResourceUtil;
import com.yupi.yc_oj_codesandbox.model.ExecuteCodeRequest;
import com.yupi.yc_oj_codesandbox.model.ExecuteCodeResponse;
import com.yupi.yc_oj_codesandbox.templatebox.PythonCodeSandboxTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;


@Component
public class PythonNativeCodeSandbox extends PythonCodeSandboxTemplate {

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }


    public static void main(String[] args) {
        PythonNativeCodeSandbox pythonNativeCodeSandbox = new PythonNativeCodeSandbox();
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "1 3"));
        String code = ResourceUtil.readStr("testCode/python/Main.py", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);
        executeCodeRequest.setLanguage("python");
        ExecuteCodeResponse executeCodeResponse = pythonNativeCodeSandbox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }
}
