package com.zf.sync.utils;

import java.util.ArrayList;
import java.util.List;

public class ExecResult {
    private boolean isSuccess;

    private List<String> results = new ArrayList<>();

    public ExecResult(boolean isSuccess, List<String> results) {
        this.isSuccess = isSuccess;
        this.results = results;
    }

    public boolean isSuccess(){
        return isSuccess;
    }

    public List<String> getResults() {
        return results;
    }

    public String getResult(){
        return results.size() == 0 ? "" : results.get(0);
    }

    public void print(){
        for (String msg : results) {
            System.out.println(msg);
        }
    }

    @Override
    public String toString(){
        StringBuilder messages = new StringBuilder();
        for (String item : results) {
            messages.append(item).append("\n");
        }
        return messages.toString();
    }
}
