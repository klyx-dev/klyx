#pragma once
#include "extension.h"
#include "include/tl/expected.hpp"
#include <cstdint>
#include <memory>
#include <optional>
#include <string>
#include <vector>

namespace klyx {

using LanguageServerId = std::string;

inline std::string to_std_string(const extension_string_t *s) {
  return std::string(reinterpret_cast<const char *>(s->ptr), s->len);
}

inline extension_string_t to_extension_string(const std::string &s) {
  extension_string_t out;
  extension_string_dup(&out, s.c_str());
  return out;
}

template <typename T> struct Result {
  T value{};
  std::string error{};
  bool ok{false};

  static Result<T> Ok(const T &val) { return {val, "", true}; }
  static Result<T> Err(const std::string &err) { return {{}, err, false}; }
};

template <> struct Result<void> {
  std::string error{};
  bool ok{false};

  static Result<void> Ok() { return {"", true}; }
  static Result<void> Err(const std::string &err) { return {err, false}; }
};

struct Command {
  std::string binary;
  std::vector<std::string> args;
  std::vector<std::pair<std::string, std::string>> env;
};

class Worktree {
private:
  extension_borrow_worktree_t handle;

public:
  Worktree(extension_borrow_worktree_t h) : handle(h) {}

  std::string root_path() const {
    extension_string_t root_path;
    extension_method_worktree_root_path(handle, &root_path);
    std::string result = to_std_string(&root_path);
    extension_string_free(&root_path);
    return result;
  }

  uint64_t id() const { return extension_method_worktree_id(handle); }

  tl::expected<std::string, std::string>
  read_text_file(const std::string &path) {
    extension_string_t content;
    extension_string_t error;
    extension_string_t p = to_extension_string(path);

    bool success =
        extension_method_worktree_read_text_file(handle, &p, &content, &error);
    extension_string_free(&p);

    if (!success) {
      std::string err_msg = to_std_string(&error);
      extension_string_free(&error);
      return tl::unexpected(err_msg);
    }

    std::string result = to_std_string(&content);
    extension_string_free(&content);
    extension_string_free(&error);
    return result;
  }

  std::optional<std::string> which(const std::string &binary_name) {
    extension_string_t out;
    extension_string_t bin = to_extension_string(binary_name);

    bool success = extension_method_worktree_which(handle, &bin, &out);
    extension_string_free(&bin);

    if (!success) {
      return std::nullopt;
    }

    std::string result = to_std_string(&out);
    extension_string_free(&out);
    return result;
  }

  std::vector<std::pair<std::string, std::string>> shell_env() const {
    extension_env_vars_t env;
    extension_method_worktree_shell_env(handle, &env);

    std::vector<std::pair<std::string, std::string>> result;
    result.reserve(env.len);

    for (size_t i = 0; i < env.len; i++) {
      auto &pair = env.ptr[i];
      result.emplace_back(to_std_string(&pair.f0), to_std_string(&pair.f1));
    }

    extension_env_vars_free(&env);
    return result;
  }
};

class Extension {
public:
  Extension() = default;
  virtual ~Extension() = default;

  virtual void init() {}
  virtual void uninstall() {}

  // Returns the command used to start the language server
  virtual Result<Command> language_server_command(
      const LanguageServerId & /*language_server_id*/, Worktree & /*worktree*/
  ) {
    return Result<Command>::Err("`language_server_command` not implemented");
  }

  // Returns the initialization options for the language server
  virtual Result<std::optional<std::string>>
  language_server_initialization_options(
      const LanguageServerId & /*language_server_id*/, Worktree & /*worktree*/
  ) {
    return Result<std::optional<std::string>>::Ok(std::nullopt);
  }

  // Returns the workspace configuration options for the language server
  virtual Result<std::optional<std::string>>
  language_server_workspace_configuration(
      const LanguageServerId & /*language_server_id*/, Worktree & /*worktree*/
  ) {
    return Result<std::optional<std::string>>::Ok(std::nullopt);
  }

  // Additional initialization options for another language server
  virtual Result<std::optional<std::string>>
  language_server_additional_initialization_options(
      const LanguageServerId & /*language_server_id*/,
      const LanguageServerId & /*target_language_server_id*/,
      Worktree & /*worktree*/
  ) {
    return Result<std::optional<std::string>>::Ok(std::nullopt);
  }

  // Additional workspace configuration for another language server
  virtual Result<std::optional<std::string>>
  language_server_additional_workspace_configuration(
      const LanguageServerId & /*language_server_id*/,
      const LanguageServerId & /*target_language_server_id*/,
      Worktree & /*worktree*/
  ) {
    return Result<std::optional<std::string>>::Ok(std::nullopt);
  }
};

} // namespace klyx

inline klyx::Extension *EXTENSION = nullptr;

inline void register_extension(std::unique_ptr<klyx::Extension> ext) {
  EXTENSION = ext.release();
}

#define REGISTER_EXTENSION(EXT_CLASS)                                          \
  extern "C" void exports_extension_init_extension() {                         \
    register_extension(std::make_unique<EXT_CLASS>());                         \
    if (EXTENSION)                                                             \
      EXTENSION->init();                                                       \
  }

inline void fill_command(const klyx::Command &cmd, extension_command_t *ret) {
  extension_string_dup(&ret->command, cmd.binary.c_str());

  if (!cmd.args.empty()) {
    ret->args.len = cmd.args.size();
    ret->args.ptr = (extension_string_t *)malloc(sizeof(extension_string_t) *
                                                 cmd.args.size());
    for (size_t i = 0; i < cmd.args.size(); ++i) {
      extension_string_dup(&ret->args.ptr[i], cmd.args[i].c_str());
    }
  } else {
    ret->args.len = 0;
    ret->args.ptr = nullptr;
  }

  if (!cmd.env.empty()) {
    ret->env.len = cmd.env.size();
    ret->env.ptr = (extension_tuple2_string_string_t *)malloc(
        sizeof(extension_tuple2_string_string_t) * cmd.env.size());
    for (size_t i = 0; i < cmd.env.size(); ++i) {
      extension_string_dup(&ret->env.ptr[i].f0, cmd.env[i].first.c_str());
      extension_string_dup(&ret->env.ptr[i].f1, cmd.env[i].second.c_str());
    }
  } else {
    ret->env.len = 0;
    ret->env.ptr = nullptr;
  }
}

#ifdef __cplusplus
extern "C" {
#endif

void exports_extension_uninstall() {
  if (!EXTENSION)
    return;
  EXTENSION->uninstall();
}

bool exports_extension_language_server_command(
    extension_string_t *language_server_id,
    extension_borrow_worktree_t worktree, extension_command_t *ret,
    extension_string_t *err) {
  if (!EXTENSION)
    return false;

  auto id = klyx::to_std_string(language_server_id);
  klyx::Worktree wt{worktree};

  auto result = EXTENSION->language_server_command(id, wt);
  if (!result.ok) {
    *err = klyx::to_extension_string(result.error);
    return false;
  }

  fill_command(result.value, ret);
  return true;
}

bool exports_extension_language_server_initialization_options(
    extension_string_t *language_server_id,
    extension_borrow_worktree_t worktree, extension_option_string_t *ret,
    extension_string_t *err) {
  if (!EXTENSION)
    return false;

  auto id = klyx::to_std_string(language_server_id);
  klyx::Worktree wt{worktree};

  auto result = EXTENSION->language_server_initialization_options(id, wt);
  if (!result.ok) {
    *err = klyx::to_extension_string(result.error);
    return false;
  }

  if (result.value.has_value()) {
    *ret = {true, klyx::to_extension_string(result.value.value())};
  } else {
    ret->is_some = false;
  }

  return true;
}

bool exports_extension_language_server_workspace_configuration(
    extension_string_t *language_server_id,
    extension_borrow_worktree_t worktree, extension_option_string_t *ret,
    extension_string_t *err) {
  if (!EXTENSION)
    return false;

  auto id = klyx::to_std_string(language_server_id);
  klyx::Worktree wt{worktree};

  auto result = EXTENSION->language_server_workspace_configuration(id, wt);
  if (!result.ok) {
    *err = klyx::to_extension_string(result.error);
    return false;
  }

  if (result.value.has_value()) {
    *ret = {true, klyx::to_extension_string(result.value.value())};
  } else {
    ret->is_some = false;
  }

  return true;
}

bool exports_extension_language_server_additional_initialization_options(
    extension_string_t *language_server_id,
    extension_string_t *target_language_server_id,
    extension_borrow_worktree_t worktree, extension_option_string_t *ret,
    extension_string_t *err) {
  if (!EXTENSION)
    return false;

  auto id = klyx::to_std_string(language_server_id);
  auto target_id = klyx::to_std_string(target_language_server_id);
  klyx::Worktree wt{worktree};

  auto result = EXTENSION->language_server_additional_initialization_options(
      id, target_id, wt);
  if (!result.ok) {
    *err = klyx::to_extension_string(result.error);
    return false;
  }

  if (result.value.has_value()) {
    *ret = {true, klyx::to_extension_string(result.value.value())};
  } else {
    ret->is_some = false;
  }

  return true;
}

bool exports_extension_language_server_additional_workspace_configuration(
    extension_string_t *language_server_id,
    extension_string_t *target_language_server_id,
    extension_borrow_worktree_t worktree, extension_option_string_t *ret,
    extension_string_t *err) {
  if (!EXTENSION)
    return false;

  auto id = klyx::to_std_string(language_server_id);
  auto target_id = klyx::to_std_string(target_language_server_id);
  klyx::Worktree wt{worktree};

  auto result = EXTENSION->language_server_additional_workspace_configuration(
      id, target_id, wt);
  if (!result.ok) {
    *err = klyx::to_extension_string(result.error);
    return false;
  }

  if (result.value.has_value()) {
    *ret = {true, klyx::to_extension_string(result.value.value())};
  } else {
    ret->is_some = false;
  }

  return true;
}

bool exports_extension_labels_for_completions(
    extension_string_t *language_server_id,
    extension_list_completion_t *completions,
    extension_list_option_code_label_t *ret, extension_string_t *err) {
  // Not implemented in klyx
  return false;
}

bool exports_extension_labels_for_symbols(
    extension_string_t *language_server_id, extension_list_symbol_t *symbols,
    extension_list_option_code_label_t *ret, extension_string_t *err) {
  // Not implemented in klyx
  return false;
}

#ifdef __cplusplus
} // extern "C"
#endif
