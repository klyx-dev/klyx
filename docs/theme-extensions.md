# Creating Theme Extensions for Klyx

This guide will help you create custom theme extensions for Klyx. Theme extensions allow you to customize the appearance of the editor and create your own unique visual styles.

## Extension Structure

A theme extension consists of the following structure:

```
your-theme-extension/
├── extension.toml
└── themes/
    └── themes.json
```

## Configuration Files

### 1. extension.toml

This file contains the metadata for your theme extension:

```toml
id = "your-theme-id"
name = "Your Theme Name"
version = "0.0.1"
schema_version = 1
authors = ["Your Name"]
description = "Description of your theme"
repository = ""

[extension]
type = "theme"
```

### 2. themes.json

This file contains the actual theme definitions. You can define multiple themes in a single extension:

```json
{
    "author": "Your Name",
    "name": "Theme Collection Name",
    "themes": [
        {
            "name": "Theme Name",
            "appearance": "dark", // or "light"
            "style": {
                // Theme properties
            }
        }
    ]
}
```

## Theme Properties

The theme style object supports the following properties:

### Base Colors
- `background`: Main background color
- `background.appearance`: Set to "opaque" for solid backgrounds
- `surface.background`: Surface background color
- `elevated_surface.background`: Elevated surface background color
- `panel.background`: Panel background color

### Editor Colors
- `editor.background`: Editor background color
- `editor.foreground`: Editor text color
- `editor.gutter.background`: Line number gutter background
- `editor.line_number`: Line number color
- `editor.active_line_number`: Active line number color
- `editor.active_line.background`: Active line background
- `editor.indent_guide`: Indent guide color
- `editor.indent_guide_active`: Active indent guide color

### Text Colors
- `text`: Primary text color
- `text.muted`: Muted text color
- `text.disabled`: Disabled text color
- `text.accent`: Accent text color
- `text.placeholder`: Placeholder text color

### Border Colors
- `border`: Primary border color
- `border.variant`: Variant border color
- `border.focused`: Focused border color
- `border.selected`: Selected border color
- `border.transparent`: Transparent border
- `border.disabled`: Disabled border color

### Element Colors
- `element.background`: Element background
- `element.hover`: Element hover state
- `element.active`: Element active state
- `element.selected`: Element selected state
- `element.disabled`: Element disabled state

### Ghost Element Colors
- `ghost_element.background`: Ghost element background
- `ghost_element.hover`: Ghost element hover state
- `ghost_element.active`: Ghost element active state
- `ghost_element.selected`: Ghost element selected state
- `ghost_element.disabled`: Ghost element disabled state

### Icon Colors
- `icon`: Primary icon color
- `icon.muted`: Muted icon color
- `icon.disabled`: Disabled icon color
- `icon.placeholder`: Placeholder icon color
- `icon.accent`: Accent icon color

### Terminal Colors
- `terminal.background`: Terminal background
- `terminal.foreground`: Terminal text color
- `terminal.ansi.black`: ANSI black
- `terminal.ansi.red`: ANSI red
- `terminal.ansi.green`: ANSI green
- `terminal.ansi.yellow`: ANSI yellow
- `terminal.ansi.blue`: ANSI blue
- `terminal.ansi.magenta`: ANSI magenta
- `terminal.ansi.cyan`: ANSI cyan
- `terminal.ansi.white`: ANSI white

### Status Colors
- `error`: Error color
- `error.background`: Error background
- `error.border`: Error border
- `warning`: Warning color
- `warning.background`: Warning background
- `warning.border`: Warning border
- `success`: Success color
- `success.background`: Success background
- `success.border`: Success border
- `info`: Info color
- `info.background`: Info background
- `info.border`: Info border

### Syntax Highlighting
```json
"syntax": {
    "keyword": {
        "color": "#color",
        "font_weight": 600
    },
    "function": {
        "color": "#color"
    },
    "string": {
        "color": "#color"
    },
    "comment": {
        "color": "#color",
        "font_style": "italic"
    },
    "variable": {
        "color": "#color"
    },
    "number": {
        "color": "#color"
    },
    "boolean": {
        "color": "#color"
    },
    "type": {
        "color": "#color"
    }
}
```

## Best Practices

1. **Color Contrast**: Ensure sufficient contrast between text and background colors for readability
2. **Consistency**: Maintain consistent color schemes throughout your theme
3. **Accessibility**: Consider color-blind users when choosing colors
4. **Testing**: Test your theme in both light and dark modes
5. **Documentation**: Include a README with screenshots and color palette information

## Example Theme

Here's a minimal example of a dark theme:

```json
{
    "author": "Your Name",
    "name": "Minimal Dark",
    "themes": [
        {
            "name": "Minimal Dark",
            "appearance": "dark",
            "style": {
                "background": "#1a1a1a",
                "background.appearance": "opaque",
                "surface.background": "#2d2d2d",
                "editor.background": "#1a1a1a",
                "editor.foreground": "#ffffff",
                "text": "#ffffff",
                "text.muted": "#888888",
                "border": "#404040",
                "syntax": {
                    "keyword": {
                        "color": "#ff79c6",
                        "font_weight": 600
                    },
                    "string": {
                        "color": "#f1fa8c"
                    }
                }
            }
        }
    ]
}
```

## Installation

Not available currently

## Contributing

Feel free to contribute your themes to the Klyx community! Make sure to:
1. Follow the theme structure guidelines
2. Include proper documentation
3. Test your theme thoroughly
4. Provide screenshots of your theme in action 