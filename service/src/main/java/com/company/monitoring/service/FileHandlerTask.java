package com.company.monitoring.service;

import com.company.monitoring.api.Handler;
import com.company.monitoring.api.File;
import lombok.ToString;

@ToString
public class FileHandlerTask extends HandlerTask<File, String> {
    public FileHandlerTask(Handler<File> handler, File file) {
        super(handler, file);
    }

    @Override
    public String getKey() {
        return getData().getPath().toString();
    }


}
