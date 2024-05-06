package com.yupi.yc_oj_codesandbox;


import com.yupi.yc_oj_codesandbox.model.ExecuteCodeRequest;
import com.yupi.yc_oj_codesandbox.model.ExecuteCodeResponse;


/**
 * 代码沙箱接口定义
 */
public interface CodeSandbox {

    /**
     * 执行代码
     *
     * @param executeCodeRequest
     * @return
     */
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
