# shellcheck disable=SC2034
force_color_prompt=yes
shopt -s checkwinsize

source "$LOCAL/bin/utils"

export PATH=/bin:/sbin:/usr/bin:/usr/sbin:/usr/games:/usr/local/bin:/usr/local/sbin:$LOCAL/bin:$PATH
export SHELL="/bin/bash"

REQUIRED="sudo nano curl command-not-found"
missing=""

for pkg in $REQUIRED; do
    if ! dpkg -s "$pkg" >/dev/null 2>&1; then
        missing="$missing $pkg"
    fi
done

if [ -n "$missing" ]; then
    info "Installing missing packages:$missing"

    if export DEBIAN_FRONTEND=noninteractive &&
        apt update -y &&
        apt install -y $missing; then

        update-command-not-found >/dev/null 2>&1 || true

        info "Packages installed."
    else
        error "Failed to install packages."
        exit 1
    fi
fi

if ! grep -q "command-not-found" /etc/bash.bashrc 2>/dev/null; then
cat >> /etc/bash.bashrc <<'EOF'

if [ -f /usr/share/command-not-found/command-not-found ]; then
    . /usr/share/command-not-found/command-not-found
fi

EOF
fi

patch_bashrc() {
    file="$1"

    [ -f "$file" ] || return 0

    if ! grep -q "KLYX_PROMPT" "$file"; then
cat >> "$file" <<'EOF'

# KLYX_PROMPT
DISTRO_ID=$(. /etc/os-release && printf "%s" "$ID")

if [ -n "$DISTRO_ID" ]; then
    PS1="${PS1//\\u@\\h/\\u@${DISTRO_ID}}"
fi

EOF
    fi
}

patch_bashrc /etc/skel/.bashrc
patch_bashrc /root/.bashrc

if ! id -u "$USER" >/dev/null 2>&1; then
    info "Adding user '$USER'..."

    useradd -m -s /bin/bash "$USER"
    passwd "$USER"

    echo "$USER ALL=(ALL:ALL) ALL" >> /etc/sudoers
fi

patch_bashrc "/home/$USER/.bashrc"

# shellcheck disable=SC2164
cd "$WKDIR" || cd "$HOME"

declare -px > /etc/profile.d/klyxenv.sh
chmod 755 /etc/profile.d/klyxenv.sh

if [ "$USER" != "root" ]; then
    exec su - "$USER" -l
fi

exec bash -l
