#include "../extension_api/extension_api.h"
#include "../extension_api/system.h"
#include <string>
#include <vector>
#include <optional>

class CppExtension : public klyx::Extension {
public:
    CppExtension() = default;
    ~CppExtension() override = default;

    void init() override {
        klyx::system::show_toast(
            "CppExtension loaded!",
            klyx::system::ToastDuration::Short
        );
    }

    void uninstall() override {
        cached_settings.reset();
    }

    klyx::Result<klyx::Command> language_server_command(
        const klyx::LanguageServerId &language_server_id,
        klyx::Worktree &worktree
    ) override {
        if (worktree.which("clangd").has_value()) {
            return klyx::Result<klyx::Command>::Ok(create_clangd_command(worktree.which("clangd").value()));
        }
        return klyx::Result<klyx::Command>::Err(
            "clangd not found. Please install clangd using: sudo apt install clangd"
        );
    }

    klyx::Result<std::optional<std::string>> language_server_initialization_options(
        const klyx::LanguageServerId &,
        klyx::Worktree &worktree
    ) override {
        std::string init_options =
            "{"
            "\"clangdFileStatus\":true,"
            "\"usePlaceholders\":true,"
            "\"completeUnimported\":true,"
            "\"semanticHighlighting\":true,"
            "\"compilationDatabaseChanges\":true"
            "}";
        return klyx::Result<std::optional<std::string>>::Ok(init_options);
    }

    klyx::Result<std::optional<std::string>> language_server_workspace_configuration(
        const klyx::LanguageServerId &,
        klyx::Worktree &worktree
    ) override {
        if (cached_settings.has_value()) {
            return klyx::Result<std::optional<std::string>>::Ok(cached_settings);
        }

        std::string config =
            "{"
            "\"clangd\":{"
            "\"arguments\":["
            "\"--background-index\","
            "\"--clang-tidy\","
            "\"--completion-style=detailed\","
            "\"--header-insertion=iwyu\","
            "\"--pch-storage=memory\","
            "\"--function-arg-placeholders\","
            "\"--log=verbose\""
            "],"
            "\"fallbackFlags\":[\"-std=c++17\",\"-Wall\",\"-Wextra\"]"
            "}"
            "}";
        cached_settings = config;
        return klyx::Result<std::optional<std::string>>::Ok(config);
    }

private:
    std::optional<std::string> cached_settings;

    klyx::Command create_clangd_command(const std::string &clangd_path) {
        klyx::Command cmd;
        cmd.binary = clangd_path;
        cmd.args = {
            "--background-index",
            "--clang-tidy",
            "--completion-style=detailed",
            "--header-insertion=iwyu",
            "--pch-storage=memory",
            "--function-arg-placeholders",
            "--pretty"
        };
        cmd.env = { {"CLANGD_FLAGS", "--background-index"} };
        return cmd;
    }
};

// Register the extension
REGISTER_EXTENSION(CppExtension);
