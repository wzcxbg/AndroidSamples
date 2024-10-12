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
    write(infp, "echo world\n", sizeof("echo world\n"));
    write(infp, "input tap 540 1000\n", sizeof( "input tap 540 1000\n"));

    read(outfp, buf, sizeof(buf));
    __android_log_print(ANDROID_LOG_ERROR, "COMMAND", "Received: %s", buf);

    read(outfp, buf, sizeof(buf));
    __android_log_print(ANDROID_LOG_ERROR, "COMMAND", "Received: %s", buf);

    close(infp);
    close(outfp);

//    const char *cmd = "echo 123\n";
//    write(fds[0], cmd, sizeof(cmd));
//    char buffer[1024]{};
//    size_t len = read(fds[1], buffer, 1024);
//    __android_log_print(ANDROID_LOG_ERROR, "COMMAND", "len %d", len);

//    int fds[2];
//    pipe(fds);
//
//    dup2(fds[1], STDOUT_FILENO);
//
//    FILE *fp = popen("su -c sh", "w");
//    char *cmd2 = "screencap -p > /data/local/tmp/ss.png\\n";
//    fwrite(cmd2, strlen(cmd2), 1, fp);
//    //截图+解码png+input

//    char buf[1024]{};
//    int len = read(fds[0], buf, 1024);
//
//    __android_log_print(ANDROID_LOG_ERROR, "COMMAND", "len %d", len);


//    system("screencap -p");
//    const int R = 0;
//    const int W = 1;
//    int fds[2], fdr[2];
//    if (pipe(fds) || pipe(fdr)) {
//        return;
//    }
//    pid_t pid = fork();
//    if (pid == -1) {
//        return;
//    }
//    if (pid == 0) {
//        dup2(fdr[W], STDOUT_FILENO);
//        uint8_t buf[1024]{};
//        int len = read(fds[R], buf, 1024);
//        if (len > 0) {
//            FILE *fp = popen("sh", "rw");
//            fwrite("su\n", sizeof("su\n"), 1, fp);
//            fwrite("screencap -p\n", sizeof("screencap -p\n"), 1, fp);
//            __android_log_print(ANDROID_LOG_ERROR, "COMMAND", "exec %s", buf);
//        }
//    }
//
//    write(fds[W], "kunn", sizeof("kunn"));
//
//    std::ostringstream strstr;
//    std::array<char, 8196> buffer{};
//    size_t len = read(fdr[R], buffer.data(), buffer.size());
//    __android_log_print(ANDROID_LOG_ERROR, "COMMAND", "read lenkkkk %d", len);
//    if (len > 0) {
//        std::string readstr(buffer.data(), len);
//        strstr << readstr;
//        __android_log_print(ANDROID_LOG_ERROR, "COMMAND", "read len%s", strstr.str().c_str());
//    }

//    FILE *pFile = popen("su -c screencap -p", "rw");
//    if (pFile == nullptr) {
//        return;
//    }
//
//    std::ostringstream strstr;
//    std::array<char, 8196> buffer{};
//    while (!feof(pFile)) {
//        size_t len = fread(buffer.data(), 1, buffer.size(), pFile);
//        __android_log_print(ANDROID_LOG_ERROR, "COMMAND", "read len %d", len);
//        if (len > 0) {
//            std::string readstr(buffer.data(), len);
//            strstr << readstr;
//        }
//    }
//    pclose(pFile);
//
//    __android_log_print(ANDROID_LOG_ERROR, "COMMAND", "%s", strstr.str().c_str());
}
