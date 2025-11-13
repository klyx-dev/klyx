# shellcheck disable=SC2034
force_color_prompt=yes
shopt -s checkwinsize

source "$LOCAL/bin/utils"

export PATH=/bin:/sbin:/usr/bin:/usr/sbin:/usr/games:/usr/local/bin:/usr/local/sbin:$LOCAL/bin:$PATH
export SHELL="/bin/bash"

REQUIRED="sudo nano curl"
missing=""
for pkg in $REQUIRED; do
    if ! dpkg -s "$pkg" >/dev/null 2>&1; then
        missing="$missing $pkg"
    fi
done

if [ -n "$missing" ]; then
    info "Installing missing packages:$missing"
    if export DEBIAN_FRONTEND=noninteractive && apt update -y && apt install -y $missing; then
      info "Packages installed."
    else
      error "Failed to install packages."
      return 1
    fi
fi

if ! id -u "$USER" >/dev/null 2>&1; then
    info "Adding user '$USER'..."
    useradd -m -s /bin/bash "$USER"
    passwd "$USER"
    echo "$USER ALL=(ALL:ALL) ALL" >> /etc/sudoers
fi

cd "$WKDIR" || cd "$HOME"

if [ "$PENDING_CMD" = false ]; then
    exec su - "$USER" -l
fi
