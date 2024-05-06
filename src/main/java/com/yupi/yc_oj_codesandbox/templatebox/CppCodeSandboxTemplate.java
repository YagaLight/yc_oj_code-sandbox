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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;


@Slf4j
public abstract class CppCodeSandboxTemplate implements CodeSandbox {

    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";
    private static final String GLOBAL_CODE_DIR_CPP_NAME = "cppCode";


    private static final String GLOBAL_CPP_NAME = "Main.cpp";

    private static final long TIME_OUT = 5000L;
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

//        1. 把用户的代码保存为文件
        File userCodeFile = saveCodeToFile(code);
//        System.out.println(userCodeFile);


          //2. 编译代码
        Message compileFileMessage = compileFile(userCodeFile);
//        System.out.println(compileFileMessage);


        // 3. 执行代码，得到输出结果
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


    private List<Message> runFile(File userCodeFile, List<String> inputList) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        List<Message> messageList = new ArrayList<>();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try{
            for (String inputArgs : inputList) {
//            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputArgs);
                String osName = System.getProperty("os.name").toLowerCase();
                String executableName;

                if (osName.contains("win")) {
                    // Windows 系统的执行命令
                    executableName = "Main.exe"; // Windows下的可执行文件后缀通常为.exe
                } else {
                    // Unix/Linux 或 macOS 系统的执行命令
                    executableName = "./Main"; // Unix/Linux 下通常需要在前面加上"./"
                }

                Path executablePath = Paths.get(userCodeParentPath, executableName);
                List<String> command = new ArrayList<>();
                command.add(executablePath.toString());
                Collections.addAll(command, inputArgs.split("\\s+")); // 安全地添加参数
//            String runCmd = executablePath.toString() + " " + inputArgs; // 构建完整的命令
                ProcessBuilder builder = new ProcessBuilder(command);
                builder.redirectErrorStream(true); // 合并输出和错误流，方便记录和调试

                Future<?> future = executor.submit(() -> {
                    try {
                        Process runProcess = builder.start();
                        return ProcessUtils.runInteractProcessAndGetMessage(runProcess, inputArgs);
                    } catch (IOException e) {
                        System.err.println("执行命令时发生错误: " + e.getMessage());
                        Message message = new Message();
                        message.setExitValue(-1);
                        message.setErrorMessage("执行命令时发生错误: " + e.getMessage());
                        return message;
                    }
                });

                try {
                    Message message = (Message) future.get(TIME_OUT, TimeUnit.MILLISECONDS);
                    System.out.println(message);
                    messageList.add(message);
                } catch (TimeoutException e) {
                    future.cancel(true); // 取消执行，中断进程
                    System.out.println("执行超时，已中断");
                    Message timeoutMessage = new Message();
                    timeoutMessage.setExitValue(-1);
                    timeoutMessage.setErrorMessage("执行超时，已中断");
                    messageList.add(timeoutMessage);
                } catch (Exception e) {
                    System.err.println("执行过程中发生错误: " + e.getMessage());
                    Message errorMessage = new Message();
                    errorMessage.setExitValue(-1);
                    errorMessage.setErrorMessage("执行过程中发生错误: " + e.getMessage());
                    messageList.add(errorMessage);
                }
            }
        }finally {
            executor.shutdownNow();
            try {
                executor.awaitTermination(60, TimeUnit.SECONDS); // 等待线程池中的任务完成
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // 重置中断状态
            }
        }


//            try {
////                Process runProcess = Runtime.getRuntime().exec(runCmd);
//                ProcessBuilder builder = new ProcessBuilder(runCmd.split("\\s+")); // 使用空格分割命令和参数
//                builder.redirectErrorStream(true); // 合并标准输出和标准错误输出
//                Process runProcess = builder.start();
//                // 超时控制
//                new Thread(() -> {
//                    try {
//                        Thread.sleep(TIME_OUT);
//                        System.out.println("超时了，中断");
//                        runProcess.destroy();
//                    } catch (InterruptedException e) {
//                        throw new RuntimeException(e);
//                    }
//                }).start();
////                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
//                ExecuteMessage executeMessage = ProcessUtils.runInteractProcessAndGetMessage(runProcess,inputArgs);
//                System.out.println(executeMessage);
//                executeMessageList.add(executeMessage);
//            } catch (Exception e) {
//                throw new RuntimeException("执行错误", e);
//            }
//        }
        return messageList;

    }

    private Message compileFile(File userCodeFile) {
        // Modify the compile command to use g++
        // 获取源文件的绝对路径和文件名，但不包括扩展名
        String sourceFilePath = userCodeFile.getAbsolutePath();
        String baseName = sourceFilePath.substring(0, sourceFilePath.lastIndexOf('.'));

        String compileCmd = String.format("g++ -std=c++11 -o %s.exe %s", baseName, userCodeFile.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            Message message = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
//            Message message = ProcessUtils.runInteractProcessAndGetMessage()
            if (message.getExitValue() != 0) {
                throw new RuntimeException("编译错误: " + message.getErrorMessage());
            }
            return message;
        } catch (IOException e) {
            // Exception handling: throw an exception with a message including the original exception
            throw new RuntimeException("编译失败: " + e.getMessage(), e);
        }

    }

    private File saveCodeToFile(String code) {
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME + File.separator + GLOBAL_CODE_DIR_CPP_NAME;
        // 判断全局代码目录是否存在，没有则新建
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }

        // 把用户的代码隔离存放
        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_CPP_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        return userCodeFile;

    }
}
