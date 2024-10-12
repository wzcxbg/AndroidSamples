#include <iostream>
#include <sstream>

#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>
#include <fcntl.h>

int popen2(const char *command, int *infp, int *outfp) {
    int p_stdin[2], p_stdout[2];
    pid_t pid;

    if (pipe(p_stdin) != 0 || pipe(p_stdout) != 0)
        return -1;

    pid = fork();
    if (pid < 0)
        return pid;
    else if (pid == 0) {
        close(p_stdin[1]);
        dup2(p_stdin[0], 0);
        close(p_stdout[0]);
        dup2(p_stdout[1], 1);
        execl("/bin/sh", "sh", "-c", command, NULL);
//        execl("/sbin/su", "su", "-c", command, NULL);
        perror("execl");
        exit(1);
    }

    if (infp == NULL)
        close(p_stdin[1]);
    else
        *infp = p_stdin[1];

    if (outfp == NULL)
        close(p_stdout[0]);
    else
        *outfp = p_stdout[0];

    return pid;
}


extern "C"
JNIEXPORT void JNICALL
Java_com_sliver_samples_MainActivity_screenCapture(JNIEnv *env, jobject thiz) {
    // 0 读 1 写

    int infp, outfp;
    char buf[128];

    if (popen2("su\n", &infp, &outfp) < 0) {
        perror("popen2");
        return ;
    }

    write(infp, "echo Hello\n", sizeof("echo Hello\n"));
    read(outfp, buf, sizeof(buf));
    __android_log_print(ANDROID_LOG_ERROR, "COMMAND", "Received: %s", buf);
    write(infp, "echo world\n", sizeof("echo world\n"));
    read(outfp, buf, sizeof(buf));
    __android_log_print(ANDROID_LOG_ERROR, "COMMAND", "Received: %s", buf);
    write(infp, "input tap 540 1000\n", sizeof( "input tap 540 1000\n"));

    sleep(5);
    write(infp, "input tap 540 1000\n", sizeof( "input tap 540 1000\n"));

    close(infp);
    close(outfp);
}
