#pragma once

#include <chrono>
#include <format>
#include <filesystem>
#include <source_location>
#include <type_traits>

#include <functional>


#if defined(_WIN32) && defined(_WINDOWS_)
#include <debugapi.h>
#elif defined(__ANDROID__)
#include <android/log.h>
#else
#include <iostream>
#endif

namespace logger_details {
    template<class T>
    struct with_loc {
        T obj;
        std::source_location loc{};

        template<class I>
        requires std::convertible_to<I, T>
        consteval with_loc(I &other,                                /*NOLINT*/
                           const std::source_location &loc =
                           std::source_location::current()
        ): obj(static_cast<T>(other)), loc(loc) {}
    };

    std::string get_format_time() {
        const std::time_t now_c = std::time(nullptr);
        std::tm tm_time{};
#ifdef _WIN32
        localtime_s(&tm_time, &now_c);
#elif defined(__ANDROID__)
        localtime_r(&now_c, &tm_time);
#endif
        std::string time = (std::ostringstream() << std::put_time(
                &tm_time, "%Y-%m-%d %H:%M:%S")).str();
        std::ostringstream oss;
        oss << std::put_time(&tm_time, "%Y-%m-%d %H:%M:%S");
        return oss.str();
    }
}


template<class... Types>
void log(logger_details::with_loc<const std::format_string<Types...>> fmt_loc, Types &&... args) {
    std::string log_time = logger_details::get_format_time();
    std::string source_file_name = std::filesystem::path(fmt_loc.loc.file_name())
            .filename().string();
    std::int32_t source_file_line = fmt_loc.loc.line();

    std::string log_msg = std::format(fmt_loc.obj, std::forward<Types>(args)...);
#if defined(_WIN32) && defined(_WINDOWS_)
    OutputDebugString(std::format("{} {}:{} {}",
                                  log_time, source_file_name,
                                  source_file_line, log_msg).c_str());
#elif defined(__ANDROID__)
    __android_log_print(ANDROID_LOG_ERROR, "LOG", "%s",
                        std::format("{}:{} {}", source_file_name, source_file_line, log_msg).c_str());
#else
    std::cout << std::format("{} {}:{} {}\n",
                             log_time, source_file_name,
                             source_file_line, log_msg);
# endif
}


namespace measure_details {
    template<class T, std::enable_if_t<!std::is_void_v<decltype(std::declval<T>()())>, int> = 0>
    auto measure_time(T fun, const char *tag, const std::source_location loc =
    std::source_location::current()) -> auto {
        auto const start = std::chrono::system_clock::now();
        auto ret = fun();
        auto const end = std::chrono::system_clock::now();
        auto total = std::chrono::duration_cast<std::chrono::milliseconds>(
                end - start).count();
        logger_details::with_loc<const std::format_string<const char *&, long long &>> with_loc_obj =
                "{} elapsed time {}";
        with_loc_obj.loc = loc;
        log(with_loc_obj, tag, total);
        return ret;
    }

    template<class T, std::enable_if_t<std::is_void_v<decltype(std::declval<T>()())>, int> = 0>
    void measure_time(T fun, const char *tag, const std::source_location loc =
    std::source_location::current()) {
        auto const start = std::chrono::system_clock::now();
        fun();
        auto const end = std::chrono::system_clock::now();
        auto total = std::chrono::duration_cast<std::chrono::milliseconds>(
                end - start).count();
        logger_details::with_loc<const std::format_string<const char *&, long long &>> with_loc_obj =
                "{} elapsed time {}";
        with_loc_obj.loc = loc;
        log(with_loc_obj, tag, total);
    }
}

#define MeasureTime(func) measure_details::measure_time(std::function([&]() { return func; }), #func)