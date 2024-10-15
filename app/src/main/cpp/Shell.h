#pragma once

#include <iostream>
#include <sstream>

#include <unistd.h>
#include <sys/wait.h>

pid_t popen2(const char *command, int *infp, int *outfp) {
    int p_stdin[2], p_stdout[2];
    if (pipe(p_stdin) != 0 || pipe(p_stdout) != 0) {
        return -1;
    }

    pid_t pid = fork();
    if (pid < 0) {
        return pid;
    } else if (pid == 0) {
        //使用命令查看子进程: ps -A --sort=STIME
        close(p_stdin[1]);
        dup2(p_stdin[0], 0);
        close(p_stdout[0]);
        dup2(p_stdout[1], 1);
        execl("/bin/sh", "sh", "-c", command, NULL);
        perror("execl");
        exit(1);
    }

    if (infp == nullptr) {
        close(p_stdin[1]);
    } else {
        *infp = p_stdin[1];
    }
    if (outfp == nullptr) {
        close(p_stdout[0]);
    } else {
        *outfp = p_stdout[0];
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
