#pragma once

#include <chrono>
#include <format>
#include <source_location>
#include <type_traits>
#include <android/log.h>

#include <functional>

namespace logger_details {
    template<class CharT, class... Args>
    struct base_fmt_str_with_loc : public std::basic_format_string<CharT, Args...> {
        std::source_location loc;

        template<class T>
        requires std::convertible_to<const T &, std::basic_string_view<CharT>>
        consteval base_fmt_str_with_loc(
                const T &strVal,
                std::source_location loc = std::source_location::current())
                : std::basic_format_string<CharT, Args...>(strVal), loc(loc) {}
    };

    template<class... Args>
    using fmt_str_with_loc = base_fmt_str_with_loc<char, std::type_identity_t<Args>...>;

    template<class... Args>
    using wfmt_str_with_loc = base_fmt_str_with_loc<wchar_t, std::type_identity_t<Args>...>;
}

template<class... Types>
void log(const logger_details::fmt_str_with_loc<Types...> fmt, Types &&... args) {
    std::chrono::time_point now = std::chrono::system_clock::now();
    std::time_t now_c = std::chrono::system_clock::to_time_t(now);

    std::tm tm_time{};
    localtime_r(&now_c, &tm_time);

    std::ostringstream oss;
    oss << std::put_time(&tm_time, "%Y-%m-%d %H:%M:%S");
    std::string time = oss.str();

    std::filesystem::path sourceFile(fmt.loc.file_name());
    std::string fileName = sourceFile.filename().string();

    std::int32_t line = fmt.loc.line();

    std::string msg = std::vformat(fmt.get(), std::make_format_args(args...));
    std::string logMsg = std::format("{}:{} {}", fileName, line, msg);
    __android_log_print(ANDROID_LOG_ERROR, "LOG", "%s", logMsg.c_str());
}

namespace measure_details {
    template<class T, std::enable_if_t<!std::is_void_v<decltype(std::declval<T>()())>, int> = 0>
    auto measure_time(T fun, const char *tag) -> auto {
        auto start = std::chrono::system_clock::now();
        auto ret = fun();
        auto end = std::chrono::system_clock::now();
        auto total = std::chrono::duration_cast<std::chrono::milliseconds>(
                end - start).count();
        log("{} elapsed time {}", tag, total);
        return ret;
    }

    template<class T, std::enable_if_t<std::is_void_v<decltype(std::declval<T>()())>, int> = 0>
    void measure_time(T fun, const char *tag) {
        auto start = std::chrono::system_clock::now();
        fun();
        auto end = std::chrono::system_clock::now();
        auto total = std::chrono::duration_cast<std::chrono::milliseconds>(
                end - start).count();
        log("{} elapsed time {}", tag, total);
    }
}

#define MeasureTime(func) measure_details::measure_time(std::function([&]() { return func; }), #func)