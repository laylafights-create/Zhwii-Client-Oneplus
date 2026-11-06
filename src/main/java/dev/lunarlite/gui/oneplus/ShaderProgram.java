package dev.lunarlite.gui.oneplus;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLContext;

/**
 * Một chương trình GLSL 1.20 (vertex + fragment). Biên dịch đúng một lần, cache vị trí uniform.
 * {@link #load} trả về null (và ghi log) nếu GPU không hỗ trợ hoặc shader lỗi - người gọi tự dùng đường dự phòng.
 */
public final class ShaderProgram {
    private static final Logger LOG = LogManager.getLogger("ZhwiiGUI");
    private static final String DIR = "shaders/oneplus/";

    private int program;
    private final Map<String, Integer> uniforms = new HashMap<String, Integer>();

    private ShaderProgram(int program) {
        this.program = program;
    }

    /** GPU có OpenGL 2.0 trở lên (cần cho GLSL). */
    public static boolean supported() {
        try {
            return GLContext.getCapabilities().OpenGL20;
        } catch (Throwable t) {
            return false;
        }
    }

    public static ShaderProgram load(String vertName, String fragName) {
        if (!supported()) return null;
        int vs = 0, fs = 0, prog = 0;
        try {
            vs = compile(GL20.GL_VERTEX_SHADER, vertName);
            fs = compile(GL20.GL_FRAGMENT_SHADER, fragName);
            prog = GL20.glCreateProgram();
            GL20.glAttachShader(prog, vs);
            GL20.glAttachShader(prog, fs);
            GL20.glLinkProgram(prog);
            if (GL20.glGetProgrami(prog, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
                throw new IllegalStateException("link " + vertName + "+" + fragName + ": "
                        + GL20.glGetProgramInfoLog(prog, 2048));
            }
            GL20.glDetachShader(prog, vs);
            GL20.glDetachShader(prog, fs);
            GL20.glDeleteShader(vs);
            GL20.glDeleteShader(fs);
            return new ShaderProgram(prog);
        } catch (Throwable t) {
            LOG.error("Khong tai duoc shader {} / {}: {}", vertName, fragName, t.toString());
            if (vs != 0) GL20.glDeleteShader(vs);
            if (fs != 0) GL20.glDeleteShader(fs);
            if (prog != 0) GL20.glDeleteProgram(prog);
            return null;
        }
    }

    private static int compile(int type, String name) throws Exception {
        String src = readSource(name);
        int id = GL20.glCreateShader(type);
        GL20.glShaderSource(id, src);
        GL20.glCompileShader(id);
        if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(id, 2048);
            GL20.glDeleteShader(id);
            throw new IllegalStateException("compile " + name + ": " + log);
        }
        return id;
    }

    private static String readSource(String name) throws Exception {
        ResourceLocation loc = new ResourceLocation("lunarlite", DIR + name);
        InputStream in = null;
        try {
            in = Minecraft.getMinecraft().getResourceManager().getResource(loc).getInputStream();
            return IOUtils.toString(in, "UTF-8");
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    public void use() {
        GL20.glUseProgram(program);
    }

    public static void stop() {
        GL20.glUseProgram(0);
    }

    private int loc(String name) {
        Integer v = uniforms.get(name);
        if (v == null) {
            v = Integer.valueOf(GL20.glGetUniformLocation(program, name));
            uniforms.put(name, v);
        }
        return v.intValue();
    }

    public void set1i(String n, int a) {
        GL20.glUniform1i(loc(n), a);
    }

    public void set1f(String n, float a) {
        GL20.glUniform1f(loc(n), a);
    }

    public void set2f(String n, float a, float b) {
        GL20.glUniform2f(loc(n), a, b);
    }

    public void set3f(String n, float a, float b, float c) {
        GL20.glUniform3f(loc(n), a, b, c);
    }

    public void set4f(String n, float a, float b, float c, float d) {
        GL20.glUniform4f(loc(n), a, b, c, d);
    }

    /** Màu ARGB (int) -> vec4 RGBA, nhân alpha với {@code alphaMul}. */
    public void setColor(String n, int argb, float alphaMul) {
        float a = ((argb >>> 24) & 255) / 255f * alphaMul;
        set4f(n, ((argb >> 16) & 255) / 255f, ((argb >> 8) & 255) / 255f, (argb & 255) / 255f, a);
    }

    public void delete() {
        if (program != 0) GL20.glDeleteProgram(program);
        program = 0;
        uniforms.clear();
    }
}
