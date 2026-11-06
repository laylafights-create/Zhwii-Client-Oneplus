# ZHWII CLIENT – OnePlus Crystal Edition

GUI kính mờ (frosted glass) cho Minecraft 1.8.9 Forge, tích hợp thẳng vào `MenuScreen` hiện có (không có GUI demo rời).

## File thêm
`src/main/java/dev/lunarlite/gui/oneplus/`
- `GUIRenderer` – cửa chính: biên dịch shader một lần, quản lý trạng thái GL, hàm vẽ tĩnh cho MenuScreen
- `ShaderProgram` – nạp/biên dịch GLSL 1.20, cache uniform
- `FramebufferManager` – 3 framebuffer phụ (ping-pong), chỉ tạo lại khi đổi kích thước
- `BlurRenderer` – Gaussian blur 2 lượt (ngang + dọc), 2 texture: nền và kính
- `RoundedRectRenderer`, `ShadowRenderer`, `GlassPanelRenderer` – SDF bo góc, bóng mềm, kính
- `ToggleRenderer`, `CategoryRenderer`, `AnimationManager`, `OnePlusTheme`

`src/main/resources/assets/lunarlite/shaders/oneplus/`
`quad.vert`, `gaussian_blur.vert/.frag`, `rounded_rect.frag`, `glass_panel.frag`, `shadow.frag`

## File sửa
- `gui/MenuScreen.java` – glass/card/toggle/sidebar dùng shader; blur bằng `BlurRenderer`; hover/press theo delta time; fade chữ+icon
- `module/ThemeModule.java` – thêm Glass Alpha, Border Alpha, Corner Radius, Shadow Blur, Blur Radius, Blur Quality (tự lưu config)

## Đường dự phòng
Nếu GPU không có GLSL / shader lỗi: `GUIRenderer.ready()` = false và MenuScreen dùng code vẽ CPU cũ. Nếu không có framebuffer: dùng post-shader `menu_blur.json` cũ.

## Build
`gradle setupDecompWorkspace` (một lần) rồi `gradle build` – jar ở `build/libs/`. Xem log `ZhwiiGUI` nếu shader báo lỗi.
