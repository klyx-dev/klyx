#pragma once
#include "extension_api.h"
#include <string>

namespace klyx {
    namespace system {
        enum class ToastDuration {
            Short = KLYX_EXTENSION_SYSTEM_TOAST_DURATION_SHORT,
            Long = KLYX_EXTENSION_SYSTEM_TOAST_DURATION_LONG
        };

        inline void show_toast(const std::string& message, ToastDuration duration) {
            auto s = to_extension_string(message);
            klyx_extension_system_show_toast(&s, static_cast<klyx_extension_system_toast_duration_t>(duration));
            extension_string_free(&s);
        }
    }
}
