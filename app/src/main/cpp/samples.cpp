#include <iostream>
#include <sstream>
#include <vector>
#include <thread>

#include <sys/wait.h>

#include <jni.h>
#include <android/log.h>

#include "ImageDecoder.h"

pid_t popen2(const char *command, int *infp, int *outfp) {
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
    else {
        *outfp = p_stdout[0];
//        int flags = fcntl(*outfp, F_GETFL, 0);
//        fcntl(*outfp, F_SETFL, flags | O_NONBLOCK);
    }

    return pid;
}

class Shell final {
    pid_t pid = -1;
    int inFd = -1;
    int outFd = -1;
public:
    explicit Shell() {
        this->pid = popen2("su", &inFd, &outFd);
    }

    std::string execute(std::string &&cmd) const {
        std::string divider = "\xff\xfe\xfd\xfc\xfb\xfa\xf9\xf8\n";
        std::string input = cmd + " && echo " + divider;
        write(inFd, input.c_str(), input.size());
        static std::array<char, 1024> buf{};
        std::ostringstream oss;
        while (true) {
            int len = read(outFd, buf.data(), buf.size());
            if (len <= 0) continue;
            std::string tmp(buf.data(), len);
            oss << tmp;
            std::string output = oss.str();
            int idx = std::max(int(output.length() - divider.size()), 0);
            if (oss.str().substr(idx) == divider) {
                break;
            }
        }
        return oss.str().substr(0, oss.str().size() - divider.size());
    }

    ~Shell() {
        close(inFd);
        close(outFd);
        waitpid(pid, nullptr, 0);
    }
};



extern "C"
JNIEXPORT void JNICALL
Java_com_sliver_samples_MainActivity_screenCapture(JNIEnv *env, jobject thiz) {
    JavaVM *jvm;
    env->GetJavaVM(&jvm);
    std::thread([=]() {
        Shell shell;
        std::string ret1 = shell.execute("screencap -p");
        __android_log_print(ANDROID_LOG_ERROR, "COMMAND", "ret1: %s %d", ret1.c_str(), ret1.size());

        std::string ret2 = shell.execute("input tap 540 1000");
        __android_log_print(ANDROID_LOG_ERROR, "COMMAND", "ret2: %s %d", ret2.c_str(), ret2.size());

        ImageDecoder decoder(jvm);
        auto bitmap = decoder.decodeBuffer(ret1.data(), ret1.size());
        __android_log_print(ANDROID_LOG_ERROR, "COMMAND", "ret3: %d %d", bitmap->width,
                            bitmap->height);
    }).detach();

//    int infp, outfp;
//    char buf[128];
//
//    //way1：sh->su->sh
//    //查看子进程：ps -A --sort=STIME
//    pid_t pid = popen2("su\n", &infp, &outfp);
//    __android_log_print(ANDROID_LOG_ERROR, "COMMAND", "pid: %d", pid);
//
//    write(infp, "input tap 540 1000\n", sizeof("input tap 540 1000\n"));
//    sleep(5);
//    write(infp, "input tap 540 1000\n", sizeof("input tap 540 1000\n"));
//
//    close(infp);
//    close(outfp);
//    pid_t wait_pid = waitpid(pid, nullptr, 0);
//    __android_log_print(ANDROID_LOG_ERROR, "COMMAND", "wait_pid: %d", wait_pid);
}
