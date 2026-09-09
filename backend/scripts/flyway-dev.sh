#!/usr/bin/env bash
set -euo pipefail

backend_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
dev_config="$backend_dir/src/main/resources/application-dev.yml"

if [[ ! -f "$dev_config" ]]; then
  echo "Missing local dev config: $dev_config" >&2
  echo "Create backend/src/main/resources/application-dev.yml from local secret storage before running dev Flyway commands." >&2
  exit 1
fi

if [[ "$#" -eq 0 ]]; then
  echo "Usage: backend/scripts/flyway-dev.sh flyway:info|flyway:baseline|flyway:migrate [maven -D options]" >&2
  exit 1
fi

goal_seen=false
for arg in "$@"; do
  case "$arg" in
    flyway:info|flyway:baseline|flyway:migrate|flyway:validate)
      goal_seen=true
      ;;
    flyway:clean)
      echo "Refusing to run flyway:clean through the dev adoption helper." >&2
      exit 1
      ;;
  esac
done

if [[ "$goal_seen" != true ]]; then
  echo "Expected an explicit Flyway Maven goal: flyway:info, flyway:baseline, flyway:migrate, or flyway:validate." >&2
  exit 1
fi

tmp_config="$(mktemp "${TMPDIR:-/tmp}/tradepilot-flyway-dev.XXXXXX.properties")"
chmod 600 "$tmp_config"
cleanup() {
  rm -f "$tmp_config"
}
trap cleanup EXIT

ruby - "$dev_config" "$tmp_config" <<'RUBY'
require "yaml"

$dev_config = ARGV.fetch(0)
tmp_config = ARGV.fetch(1)

def split_placeholder(inner)
  depth = 0
  inner.chars.each_with_index do |char, index|
    if char == "$" && inner[index + 1] == "{"
      depth += 1
    elsif char == "}" && depth.positive?
      depth -= 1
    elsif char == ":" && depth.zero?
      return [inner[0...index], inner[(index + 1)..]]
    end
  end
  [inner, nil]
end

def resolve(value)
  text = value.to_s
  return text unless text.start_with?("${") && text.end_with?("}")

  inner = text[2...-1]
  env_name, fallback = split_placeholder(inner)
  env_value = ENV[env_name]
  return env_value if env_value && !env_value.empty?
  return resolve(fallback) if fallback

  abort("Missing required environment variable #{env_name} while resolving #{$dev_config}")
end

def property_escape(value)
  value.to_s
       .gsub("\\", "\\\\\\\\")
       .gsub("\n", "\\n")
       .gsub("\r", "\\r")
end

yaml = File.read($dev_config)
config = begin
  YAML.safe_load(yaml, aliases: true)
rescue ArgumentError
  YAML.safe_load(yaml, [], [], true)
end
datasource = config.fetch("spring").fetch("datasource")

url = resolve(datasource.fetch("url"))
user = resolve(datasource.fetch("username"))
password = resolve(datasource.fetch("password"))
driver = resolve(datasource.fetch("driver-class-name", "org.postgresql.Driver"))

File.open(tmp_config, "w", 0o600) do |file|
  file.puts("flyway.url=#{property_escape(url)}")
  file.puts("flyway.user=#{property_escape(user)}")
  file.puts("flyway.password=#{property_escape(password)}")
  file.puts("flyway.driver=#{property_escape(driver)}")
  file.puts("flyway.locations=filesystem:src/main/resources/db/migration")
end
RUBY

cd "$backend_dir"
mvn -Dflyway.configFiles="$tmp_config" "$@"
