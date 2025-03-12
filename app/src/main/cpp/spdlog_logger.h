#pragma once

#include <format>
#include <source_location>

#include <spdlog/spdlog.h>
#include <spdlog/sinks/android_sink.h>

namespace logger ::_details {
    template<class T>
    struct with_loc {
        T obj;
        std::source_location loc{};

        template<class I>
        requires std::convertible_to<I, T>
        consteval with_loc(I &other,                        /*NOLINT*/
                           std::source_location &&loc =
                           std::source_location::current()
        ): obj(static_cast<T>(other)), loc(loc) {}
    };

    std::shared_ptr<spdlog::logger> logger = []() {
        std::shared_ptr<spdlog::sinks::android_sink_mt> android_sink =
                std::make_shared<spdlog::sinks::android_sink_mt>("spdlog");
        android_sink->set_pattern("%s:%# %v");

        std::vector<spdlog::sink_ptr> sinks = {android_sink};
        std::shared_ptr<spdlog::logger> logger =
                std::make_shared<spdlog::logger>("global", sinks.begin(), sinks.end());
        spdlog::set_default_logger(logger);
        return logger;
    }();
}

namespace logger {
    template<typename... Args>
    inline void debug(_details::with_loc<fmt::format_string<Args...>> fmt_loc, Args &&... args) {
        spdlog::source_loc source_loc = {fmt_loc.loc.file_name(),
                                         (int) fmt_loc.loc.line(),
                                         fmt_loc.loc.function_name()};
        _details::logger->log(source_loc, spdlog::level::debug,
                              fmt_loc.obj, std::forward<Args>(args)...);
    }

    template<typename... Args>
    inline void info(_details::with_loc<fmt::format_string<Args...>> fmt_loc, Args &&... args) {
        spdlog::source_loc source_loc = {fmt_loc.loc.file_name(),
                                         (int) fmt_loc.loc.line(),
                                         fmt_loc.loc.function_name()};
        _details::logger->log(source_loc, spdlog::level::info,
                              fmt_loc.obj, std::forward<Args>(args)...);
    }

    template<typename... Args>
    inline void warn(_details::with_loc<fmt::format_string<Args...>> fmt_loc, Args &&... args) {
        spdlog::source_loc source_loc = {fmt_loc.loc.file_name(),
                                         (int) fmt_loc.loc.line(),
                                         fmt_loc.loc.function_name()};
        _details::logger->log(source_loc, spdlog::level::warn,
                              fmt_loc.obj, std::forward<Args>(args)...);
    }

    template<typename... Args>
    inline void error(_details::with_loc<fmt::format_string<Args...>> fmt_loc, Args &&... args) {
        spdlog::source_loc source_loc = {fmt_loc.loc.file_name(),
                                         (int) fmt_loc.loc.line(),
                                         fmt_loc.loc.function_name()};
        _details::logger->log(source_loc, spdlog::level::err,
                              fmt_loc.obj, std::forward<Args>(args)...);
    }
}

namespace logger::_details {
    template<class T, std::enable_if_t<!std::is_void_v<decltype(std::declval<T>()())>, int> = 0>
    auto measure_time(T fun, const char *tag, const std::source_location loc =
    std::source_location::current()) -> auto {
        auto const start = std::chrono::system_clock::now();
        auto ret = fun();
        auto const end = std::chrono::system_clock::now();
        auto total = std::chrono::duration_cast<std::chrono::milliseconds>(
                end - start).count();

        with_loc<fmt::format_string<const char *&, long long &>> with_loc_obj =
                "{} elapsed time {}";
        with_loc_obj.loc = loc;
        ::logger::debug(with_loc_obj, tag, total);
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
        with_loc<fmt::format_string<const char *&, long long &>> with_loc_obj =
                "{} elapsed time {}";
        with_loc_obj.loc = loc;
        ::logger::debug(with_loc_obj, tag, total);
    }
}

#define MeasureTime(func) ::logger::_details::measure_time(std::function([&]() { return func; }), #func)
