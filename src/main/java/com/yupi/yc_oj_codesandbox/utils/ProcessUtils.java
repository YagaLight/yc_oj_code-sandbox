package com.yupi.yc_oj_codesandbox.utils;

import com.yupi.yc_oj_codesandbox.model.Message;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.StopWatch;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 进程工具类
 */
public class ProcessUtils {

    /**
     * 执行进程并获取信息
     *
     * @param runProcess
     * @param opName
     * @return
     */
    public static Message runProcessAndGetMessage(Process runProcess, String opName) {
        Message message = new Message();

        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            // 等待程序执行，获取错误码
            int exitValue = runProcess.waitFor();
            message.setExitValue(exitValue);
            // 正常退出
            if (exitValue == 0) {
                System.out.println(opName + "成功");
                // 分批获取进程的正常输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                List<String> outputStrList = new ArrayList<>();
                // 逐行读取
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    outputStrList.add(compileOutputLine);
                }
                message.setMessage(StringUtils.join(outputStrList, "\n"));
            } else {
                // 异常退出
                System.out.println(opName + "失败，错误码： " + exitValue);
                // 分批获取进程的正常输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                List<String> outputStrList = new ArrayList<>();
                // 逐行读取
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
                    outputStrList.add(compileOutputLine);
                }
                message.setMessage(StringUtils.join(outputStrList, "\n"));

                // 分批获取进程的错误输出
                BufferedReader errorBufferedReader = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));
                // 逐行读取
                List<String> errorOutputStrList = new ArrayList<>();
                // 逐行读取
                String errorCompileOutputLine;
                while ((errorCompileOutputLine = errorBufferedReader.readLine()) != null) {
                    errorOutputStrList.add(errorCompileOutputLine);
                }
                message.setErrorMessage(StringUtils.join(errorOutputStrList, "\n"));
            }
            stopWatch.stop();
            message.setTime(stopWatch.getLastTaskTimeMillis());
        } catch (Exception e) {
            e.printStackTrace();
            message.setExitValue(-1); // 设置特殊错误码以表示出现异常
            message.setErrorMessage("在执行过程中发生异常: " + e.getMessage());
        }
        return message;
    }

    /**
     * 执行交互式进程并获取信息
     *
     * @param runProcess
     * @param args
     * @return
     */
    public static Message runInteractProcessAndGetMessage(Process runProcess, String args) {
        Message message = new Message();

        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            // 向控制台输入程序
            OutputStream outputStream = runProcess.getOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
//            String[] s = args.split(" ");
//            String join = StrUtil.join("\n", s) + "\n";
//            outputStreamWriter.write(join);
            outputStreamWriter.write(args + "\n");
            // 相当于按了回车，执行输入的发送
            outputStreamWriter.flush();

            // 分批获取进程的正常输出
            InputStream inputStream = runProcess.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            List<String> outputList = new ArrayList<>();
//            StringBuilder compileOutputStringBuilder = new StringBuilder();
            // 逐行读取
            String compileOutputLine;
            while ((compileOutputLine = bufferedReader.readLine()) != null) {
//                compileOutputStringBuilder.append(compileOutputLine);
//                compileOutputStringBuilder.append(compileOutputLine).append("\n");
                outputList.add(compileOutputLine);
            }
//            message.setMessage(compileOutputStringBuilder.toString());
            message.setMessage(StringUtils.join(outputList,"\n"));
            // 记得资源的释放，否则会卡死
            int exitValue = runProcess.waitFor();
            message.setExitValue(exitValue);

            // 如果进程异常退出，获取错误输出
            if (exitValue != 0) {
                List<String> outputErrorList = new ArrayList<>();
//                StringBuilder errorOutputBuilder = new StringBuilder();
                while ((compileOutputLine = bufferedReader.readLine()) != null) {
//                    errorOutputBuilder.append(compileOutputLine).append("\n");
                    outputErrorList.add(compileOutputLine);
                }
//                message.setErrorMessage(errorOutputBuilder.toString());
                message.setMessage(StringUtils.join(outputErrorList,"\n"));
            }

            outputStreamWriter.close();
            outputStream.close();
            inputStream.close();
            runProcess.destroy();
            stopWatch.stop();
            message.setTime(stopWatch.getLastTaskTimeMillis());
        } catch (Exception e) {
            message.setExitValue(-1);
            message.setErrorMessage("执行异常: " + e.getMessage());
            e.printStackTrace();
        } finally {
            runProcess.destroy(); // 确保进程被销毁
        }
        return message;
    }
}
