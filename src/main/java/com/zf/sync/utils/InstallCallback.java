package com.zf.sync.utils;

import java.util.List;

public interface InstallCallback {
    void onSuccess();

    void onFailed(List<String> errors);
}