package com.yupi.yc_oj_codesandbox.templatebox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.yupi.yc_oj_codesandbox.CodeSandbox;
import com.yupi.yc_oj_codesandbox.model.ExecuteCodeRequest;
import com.yupi.yc_oj_codesandbox.model.ExecuteCodeResponse;
import com.yupi.yc_oj_codesandbox.model.JudgeInfo;
import com.yupi.yc_oj_codesandbox.model.Message;
import com.yupi.yc_oj_codesandbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;


@Slf4j
public abstract class PythonCodeSandboxTemplate implements CodeSandbox {

    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";
    private static final String GLOBAL_CODE_DIR_PYTHON_NAME = "pythonCode";


    private static final String GLOBAL_PYTHON_NAME = "Main.py";

    private static final long TIME_OUT = 5000L;
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

//        1. 把用户的代码保存为文件
        File userCodeFile = saveCodeToFile(code);
        System.out.println(userCodeFile);


        // 2. 执行代码，得到输出结果
        List<Message> messageList = runFile(userCodeFile, inputList);
        System.out.println(messageList);
        //4.获取输出结果
        ExecuteCodeResponse outputResponse = getOutputResponse(messageList);
        System.out.println(outputResponse);

        //5.文件清理
        boolean b = deleteFile(userCodeFile);
        if (!b) {
            log.error("deleteFile error, userCodeFilePath = {}", userCodeFile.getAbsolutePath());
        }

        return outputResponse;
    }


    private File saveCodeToFile(String code) {
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME + File.separator + GLOBAL_CODE_DIR_PYTHON_NAME;
        FileUtil.mkdir(globalCodePathName);

        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_PYTHON_NAME;
        return FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
    }

    private List<Message> runFile(File userCodeFile, List<String> inputList) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        List<Message> messageList = new ArrayList<>();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            for (String inputArgs : inputList) {
                List<String> command = new ArrayList<>();
                command.add("python"); // or "python3", depending on the environment
                command.add(userCodeFile.getAbsolutePath());
                command.addAll(Arrays.asList(inputArgs.split("\\s+"))); // Split inputArgs and add to command

                ProcessBuilder builder = new ProcessBuilder(command);
                builder.redirectErrorStream(true); // Combine stdout and stderr

                Future<?> future = executor.submit(() -> {
                    try {
                        Process process = builder.start();
                        return ProcessUtils.runInteractProcessAndGetMessage(process, inputArgs);
                    } catch (IOException e) {
                        Message message = new Message();
                        message.setExitValue(-1);
                        message.setErrorMessage("运行错误: " + e.getMessage());
                        return message;
                    }
                });

                try {
                    Message message = (Message) future.get(TIME_OUT, TimeUnit.MILLISECONDS);
                    messageList.add(message);
                } catch (TimeoutException e) {
                    future.cancel(true);
                    Message timeoutMessage = new Message();
                    timeoutMessage.setExitValue(-1);
                    timeoutMessage.setErrorMessage("运行超时");
                    messageList.add(timeoutMessage);
                } catch (Exception e) {
                    Message errorMessage = new Message();
                    errorMessage.setExitValue(-1);
                    errorMessage.setErrorMessage("运行失败: " + e.getMessage());
                    messageList.add(errorMessage);
                }
            }
        } finally {
            executor.shutdownNow();
            try {
                executor.awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return messageList;
    }

    /**
     * 4、获取输出结果
     * @param messageList
     * @return
     */
    public ExecuteCodeResponse getOutputResponse(List<Message> messageList) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();
        // 取用时最大值，便于判断是否超时
        long maxTime = 0;
        for (Message message : messageList) {
            String errorMessage = message.getErrorMessage();
            if (StrUtil.isNotBlank(errorMessage)) {
                executeCodeResponse.setMessage(errorMessage);
                // 用户提交的代码执行中存在错误
                executeCodeResponse.setStatus(3);
                break;
            }
            outputList.add(message.getMessage());
            Long time = message.getTime();
            if (time != null) {
                maxTime = Math.max(maxTime, time);
            }
        }
        // 正常运行完成
        if (outputList.size() == messageList.size()) {
            executeCodeResponse.setStatus(1);
        }
        executeCodeResponse.setOutputList(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        // 要借助第三方库来获取内存占用，非常麻烦，此处不做实现
//        judgeInfo.setMemory();
        executeCodeResponse.setJudgeInfo(judgeInfo);
        return executeCodeResponse;
    }

    /**
     * 5、删除文件
     * @param userCodeFile
     * @return
     */
    public boolean deleteFile(File userCodeFile) {
        if (userCodeFile.getParentFile() != null) {
            String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (del ? "成功" : "失败"));
            return del;
        }
        return true;
    }

}
