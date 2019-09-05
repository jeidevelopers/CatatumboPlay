package com.ufpso.catatumboplay.util.download;

public interface DownloadCompleteListener {
    void downloadCompleted(int adminVideoId);

    void downloadCancelled(int adminVideoId);
}
