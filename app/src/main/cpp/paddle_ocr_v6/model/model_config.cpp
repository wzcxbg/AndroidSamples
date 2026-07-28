#include "model/model_config.h"

#include <algorithm>
#include <cstddef>
#include <sstream>

#include "model/types.h"
#include "util/yaml_utils.h"

namespace paddle_ocr {
namespace {

std::string trim(std::string_view value) {
  const auto begin = value.find_first_not_of(" \t");
  if (begin == std::string_view::npos) {
    return {};
  }
  const auto end = value.find_last_not_of(" \t");
  return std::string(value.substr(begin, end - begin + 1));
}

std::string replaceAll(std::string value, std::string_view from,
                       std::string_view to) {
  std::size_t position = 0;
  while ((position = value.find(from, position)) != std::string::npos) {
    value.replace(position, from.size(), to);
    position += to.size();
  }
  return value;
}

std::string parseYamlListScalar(std::string_view rawValue) {
  const auto firstNonSpace = rawValue.find_first_not_of(' ');
  std::string value(firstNonSpace == std::string_view::npos
                        ? std::string_view{}
                        : rawValue.substr(firstNonSpace));
  if (value.size() >= 2 && value.front() == '\'' && value.back() == '\'') {
    return replaceAll(value.substr(1, value.size() - 2), "''", "'");
  }
  if (value.size() >= 2 && value.front() == '"' && value.back() == '"') {
    value = value.substr(1, value.size() - 2);
    value = replaceAll(std::move(value), "\\\"", "\"");
    value = replaceAll(std::move(value), "\\\\", "\\");
    value = replaceAll(std::move(value), "\\n", "\n");
    value = replaceAll(std::move(value), "\\r", "\r");
    return replaceAll(std::move(value), "\\t", "\t");
  }
  return value;
}

std::vector<std::string> splitLines(std::string_view content) {
  std::string normalized(content);
  normalized = replaceAll(std::move(normalized), "\r\n", "\n");
  normalized = replaceAll(std::move(normalized), "\r", "\n");

  std::vector<std::string> lines;
  std::istringstream stream(normalized);
  for (std::string line; std::getline(stream, line);) {
    lines.push_back(std::move(line));
  }
  return lines;
}

std::size_t findCharacterDictLine(const std::vector<std::string> &lines,
                                  std::size_t startLine,
                                  std::size_t postProcessIndent) {
  for (std::size_t index = startLine; index < lines.size(); ++index) {
    const std::string content = trim(lines[index]);
    if (content.empty() || content.starts_with('#')) {
      continue;
    }
    const std::size_t indent = YamlUtils::leadingSpaces(lines[index]);
    if (indent <= postProcessIndent) {
      break;
    }
    if (content == "character_dict:") {
      return index;
    }
  }
  return lines.size();
}

} // namespace

ModelConfig ModelConfig::parse(std::string_view content) {
  const std::vector<std::string> lines = splitLines(content);
  const auto postProcess =
      std::find_if(lines.begin(), lines.end(), [](const std::string &line) {
        return trim(line) == "PostProcess:";
      });
  if (postProcess == lines.end()) {
    throw OCRError(OCRErrorCode::ConfigParseFailed,
                   "Failed to parse config: missing PostProcess");
  }

  const std::size_t postIndex =
      static_cast<std::size_t>(std::distance(lines.begin(), postProcess));
  const std::size_t dictIndex = findCharacterDictLine(
      lines, postIndex + 1, YamlUtils::leadingSpaces(*postProcess));
  if (dictIndex == lines.size()) {
    throw OCRError(OCRErrorCode::ConfigParseFailed,
                   "Failed to parse config: missing character_dict");
  }

  const std::size_t dictIndent = YamlUtils::leadingSpaces(lines[dictIndex]);
  std::vector<std::string> characters;
  for (std::size_t index = dictIndex + 1; index < lines.size(); ++index) {
    const std::string &line = lines[index];
    const std::size_t indent = YamlUtils::leadingSpaces(line);
    const std::string_view lineContent(line.data() + indent,
                                       line.size() - indent);
    const std::string keyLikeContent = trim(lineContent);
    if (keyLikeContent.empty() || keyLikeContent.starts_with('#')) {
      continue;
    }
    if (!lineContent.starts_with('-')) {
      if (indent <= dictIndent) {
        break;
      }
      continue;
    }
    characters.push_back(parseYamlListScalar(lineContent.substr(1)));
  }

  if (characters.empty()) {
    throw OCRError(OCRErrorCode::ConfigParseFailed,
                   "Failed to parse config: empty character_dict");
  }
  if (characters.back() != " ") {
    characters.emplace_back(" ");
  }
  return ModelConfig{.characterList = std::move(characters)};
}

} // namespace paddle_ocr
