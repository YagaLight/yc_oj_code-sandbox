package com.yupi.yc_oj_codesandbox.model;

import lombok.Data;

/**
 * 进程执行信息
 */
@Data
public class Message {

    private Integer exitValue;

    private String message;

    private String errorMessage;

    private Long time;

    private Long memory;
}
