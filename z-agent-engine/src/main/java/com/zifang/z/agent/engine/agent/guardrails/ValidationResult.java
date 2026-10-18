package com.zifang.z.agent.engine.agent.guardrails;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 参数校验结果
 */
public class ValidationResult implements Serializable {
    private final boolean valid;
    private final List<String> errors;

    private ValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors;
    }

    public static ValidationResult ok() {
        return new ValidationResult(true, new ArrayList<>());
    }

    public static ValidationResult error(String msg) {
        List<String> list = new ArrayList<>();
        list.add(msg);
        return new ValidationResult(false, list);
    }

    public static ValidationResult error(List<String> msgs) {
        return new ValidationResult(false, msgs);
    }

    public boolean isValid() {
        return valid;
    }

    public List<String> getErrors() {
        return errors;
    }

    public String getErrorMessage() {
        return errors.isEmpty() ? "" : errors.get(0);
    }
}