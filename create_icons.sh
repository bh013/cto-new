#!/bin/bash

# Create simple placeholder PNG icons using ImageMagick if available, or use touch for placeholder files
create_icon() {
    local size=$1
    local dir=$2
    
    mkdir -p "$dir"
    
    # Try to create a simple colored square as a placeholder
    if command -v convert &> /dev/null; then
        convert -size ${size}x${size} xc:#3DDC84 "$dir/ic_launcher.png"
        convert -size ${size}x${size} xc:#3DDC84 "$dir/ic_launcher_round.png"
        convert -size ${size}x${size} xc:#FFFFFF "$dir/ic_launcher_foreground.png"
    else
        # If ImageMagick is not available, create empty files
        touch "$dir/ic_launcher.png"
        touch "$dir/ic_launcher_round.png"
        touch "$dir/ic_launcher_foreground.png"
    fi
}

create_icon 48 "app/src/main/res/mipmap-mdpi"
create_icon 72 "app/src/main/res/mipmap-hdpi"
create_icon 96 "app/src/main/res/mipmap-xhdpi"
create_icon 144 "app/src/main/res/mipmap-xxhdpi"
create_icon 192 "app/src/main/res/mipmap-xxxhdpi"

echo "Icons created successfully"
