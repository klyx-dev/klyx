set -e

export PATH=/bin:/sbin:/usr/bin:/usr/sbin:/usr/local/bin:/usr/local/sbin:/system/bin:/system/xbin

# Fix DNS
# echo "nameserver 8.8.8.8" > /etc/resolv.conf

REQUIRED="sudo nano curl"
missing=""
for pkg in $REQUIRED; do
    if ! dpkg -s "$pkg" >/dev/null 2>&1; then
        missing="$missing $pkg"
    fi
done

if [ -n "$missing" ]; then
    echo -e "\e[33;1mInstalling missing packages:$missing\e[0m"
    apt update && apt install -y $missing
    echo -e "\e[32;1mPackages installed.\e[0m"
fi

if ! id -u "$USER" >/dev/null 2>&1; then
    echo -e "\e[35;1mAdding user '$USER'...\e[0m"
    adduser "$USER"
    #echo "$USER:$USER" | chpasswd
    #echo "$USER ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers
fi

fix_group() {
    for gid in 1077 3003 9997 $CACHE_GID $EXT_GID; do
        if ! getent group "$gid" >/dev/null; then
            echo "android$gid:x:$gid:" >> /etc/group
            #echo -e "\e[33mGroup with GID $gid added.\e[0m"
        fi
    done
}

fix_group

echo -e "\e[33mUse \e[36msu $USER\e[33m to login.\e[0m\n"

#exec su "$USER"
exec bash
