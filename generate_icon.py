from PIL import Image, ImageDraw, ImageFont, ImageFilter

def create_gradient(width, height, start_color, end_color):
    base = Image.new('RGB', (width, height), start_color)
    top = Image.new('RGB', (width, height), end_color)
    mask = Image.new('L', (width, height))
    mask_data = []
    for y in range(height):
        for x in range(width):
            mask_data.append(int(255 * (y / height)))
    mask.putdata(mask_data)
    base.paste(top, (0, 0), mask)
    return base

def create_icon(size=512):
    # Professional Blue Gradient
    bg_color_start = (33, 150, 243) # Material Blue 500
    bg_color_end = (25, 118, 210)   # Material Blue 700
    
    image = create_gradient(size, size, bg_color_start, bg_color_end)
    draw = ImageDraw.Draw(image)
    
    # White scanner frame
    margin = size // 5
    thickness = size // 15
    line_len = size // 4
    
    color = (255, 255, 255)
    
    # Top-Left corner
    draw.line([(margin, margin), (margin + line_len, margin)], fill=color, width=thickness)
    draw.line([(margin, margin), (margin, margin + line_len)], fill=color, width=thickness)
    
    # Top-Right corner
    draw.line([(size - margin - line_len, margin), (size - margin, margin)], fill=color, width=thickness)
    draw.line([(size - margin, margin), (size - margin, margin + line_len)], fill=color, width=thickness)
    
    # Bottom-Left corner
    draw.line([(margin, size - margin), (margin + line_len, size - margin)], fill=color, width=thickness)
    draw.line([(margin, size - margin - line_len), (margin, size - margin)], fill=color, width=thickness)
    
    # Bottom-Right corner
    draw.line([(size - margin - line_len, size - margin), (size - margin, size - margin)], fill=color, width=thickness)
    draw.line([(size - margin, size - margin - line_len), (size - margin, size - margin)], fill=color, width=thickness)
    
    # Central Laser Line (Red with glow effect simulated by multiple lines)
    center_y = size // 2
    laser_color = (255, 82, 82) # Red accent
    
    # Glow
    draw.line([(margin + thickness, center_y), (size - margin - thickness, center_y)], fill=(255, 0, 0), width=thickness)
    
    # Save base icon
    image.save("app_icon_generated.png")
    print("Icon generated: app_icon_generated.png")

    # Define mipmap folders and sizes
    mipmaps = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192
    }

    base_path = "android-scanner/app/src/main/res"
    
    import os
    
    for folder, size in mipmaps.items():
        resized = image.resize((size, size), Image.Resampling.LANCZOS)
        folder_path = os.path.join(base_path, folder)
        if not os.path.exists(folder_path):
            os.makedirs(folder_path)
            
        file_path = os.path.join(folder_path, "ic_launcher.png")
        resized.save(file_path)
        print(f"Saved {file_path} ({size}x{size})")

    # Also save round icon if needed (using same square for now)
    for folder, size in mipmaps.items():
        resized = image.resize((size, size), Image.Resampling.LANCZOS)
        folder_path = os.path.join(base_path, folder)
        file_path = os.path.join(folder_path, "ic_launcher_round.png")
        resized.save(file_path)
        print(f"Saved {file_path} ({size}x{size})")

if __name__ == "__main__":
    create_icon()
