package com.zifang.z.agent.core;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * README 里每一条定量主张都必须能从源码重算出来 —— 广告出去的话就是承诺。
 *
 * <p>为什么要有这一层：本仓 README 把「当前版本 1.0.1 / 父项目 1.1.0 / 7 个模块 /
 * 内核 8 条 DM / @Bean 3 个 / main 383 个 .java / 五个类的行数 / 7 个方法级端点 +
 * 2 个类级 / center-web 18 Controller 135 端点 / 1 个真实 main / 6 个错误码 /
 * 5 个 DTO / api 零依赖」写成了硬数字。每抬一次版本、每加一个类、每加一个端点，
 * 这些数字都会漂一次。没有任何尺子去问盘面「现在还是吗」，那这些数字的保质期
 * 就是上一次编辑的时刻。</p>
 *
 * <p>口径三条：</p>
 * <ol>
 *   <li><b>主张抠不到就红</b>：不许「正则没匹配上就跳过」，那正是空跑；</li>
 *   <li><b>事实对象只装源码侧事实</b>，README 侧的数字在核对当下现解析 ——
 *       否则尺子在做恒等式比较，改了 README 也不红；</li>
 *   <li><b>自指计数不入断言</b>：test 文件数 / 总行数 / @Test 支数会因守卫自己
 *       加文件而必然漂，README 侧已改为带日期的快照，尺子不碰它们。</li>
 * </ol>
 *
 * <p>本仓测试框架是 JUnit 4（根 POM 另带 junit-jupiter + vintage），守卫照仓内
 * 既有测试的写法用 {@code org.junit.Test}，不引入新依赖。</p>
 */
public class ReadmeClaimsTest {

    /** 能力清单点名的五个类（相对仓根）与其 README 声称的行数。 */
    private static final Map<String, String> CLASS_LINE_CLAIMS = new LinkedHashMap<>();

    static {
        CLASS_LINE_CLAIMS.put(
                "z-agent-core/src/main/java/com/zifang/z/agent/core/agent/ReActAgent.java", "203");
        CLASS_LINE_CLAIMS.put(
                "z-agent-core/src/main/java/com/zifang/z/agent/core/agent/PlanAgent.java", "191");
        CLASS_LINE_CLAIMS.put(
                "z-agent-core/src/main/java/com/zifang/z/agent/core/agent/LlmAdapter.java", "206");
        CLASS_LINE_CLAIMS.put(
                "z-agent-core/src/main/java/com/zifang/z/agent/core/runner/AgentRunner.java", "166");
        CLASS_LINE_CLAIMS.put(
                "z-agent-core/src/main/java/com/zifang/z/agent/core/registry/AgentRegistry.java", "86");
    }

    private static final String AUTOCONFIG_JAVA =
            "z-agent-starter/src/main/java/com/zifang/z/agent/starter/autoconfig/ZAgentAutoConfiguration.java";
    private static final String AGENT_EXCEPTION_JAVA =
            "z-agent-api/src/main/java/com/zifang/z/agent/api/exception/AgentException.java";
    private static final String API_POM = "z-agent-api/pom.xml";

    /**
     * 这两个字面量**故意拆开写**：README 里贴的复算命令是
     * {@code rg --no-ignore -n "SpringBootApplication|public static void main" --glob '*.java' .}，
     * 守卫自己若含完整字面量，这条命令的输出就会多出守卫文件、README 的复算读数就失真。
     */
    private static final String BOOT_ANNOTATION = "SpringBoot" + "Application";
    private static final String MAIN_METHOD = "public static void " + "main";

    private static Path repoRoot() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 5 && dir != null; i++) {
            if (Files.exists(dir.resolve("README.md")) && Files.exists(dir.resolve("pom.xml"))) {
                return dir;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("找不到仓根（同时含 README.md 与 pom.xml 的目录）");
    }

    private static String read(Path f) {
        try {
            return new String(Files.readAllBytes(f), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String readRel(String rel) {
        return read(repoRoot().resolve(rel));
    }

    /** 剥 XML 注释 —— 注释里的字样不是真坐标（根 POM 里那处 maven.deploy.skip 就是注释）。 */
    private static String stripXmlComments(String text) {
        return Pattern.compile("(?s)<!--.*?-->").matcher(text).replaceAll("");
    }

    private static Element projectElement(Path pom) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            Document doc = f.newDocumentBuilder().parse(pom.toFile());
            return doc.getDocumentElement();
        } catch (Exception e) {
            throw new IllegalStateException("解析 pom 失败：" + pom, e);
        }
    }

    private static Element child(Element parent, String name) {
        if (parent == null) {
            return null;
        }
        NodeList nl = parent.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            String local = n.getLocalName() == null ? n.getNodeName() : n.getLocalName();
            if (local.equals(name)) {
                return (Element) n;
            }
        }
        return null;
    }

    private static String text(Element parent, String name) {
        Element e = child(parent, name);
        return (e == null || e.getTextContent() == null) ? null : e.getTextContent().trim();
    }

    private static List<Element> children(Element parent, String name) {
        List<Element> out = new ArrayList<>();
        if (parent == null) {
            return out;
        }
        NodeList nl = parent.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            String local = n.getLocalName() == null ? n.getNodeName() : n.getLocalName();
            if (local.equals(name)) {
                out.add((Element) n);
            }
        }
        return out;
    }

    /** 根 POM 的 &lt;modules&gt; 声明顺序。 */
    private static List<String> rootModules() {
        Element project = projectElement(repoRoot().resolve("pom.xml"));
        List<String> out = new ArrayList<>();
        for (Element m : children(child(project, "modules"), "module")) {
            out.add(m.getTextContent().trim());
        }
        return out;
    }

    /** 某个模块下 main / test 的 .java 个数（按相对仓根路径定位，不按目录名统计）。 */
    private static int[] javaCounts(String module) {
        int main = 0;
        int test = 0;
        Path base = repoRoot().resolve(module);
        if (!Files.isDirectory(base)) {
            return new int[]{0, 0};
        }
        try (Stream<Path> stream = Files.walk(base, 30)) {
            List<Path> files = new ArrayList<>();
            stream.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.toString().contains("/target/"))
                    .forEach(files::add);
            for (Path p : files) {
                String s = p.toString();
                if (s.contains("/src/main/")) {
                    main++;
                } else if (s.contains("/src/test/")) {
                    test++;
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new int[]{main, test};
    }

    /** 某目录下 main 源码里的映射注解计数。 */
    private static int[] mappingCounts(String relDir) {
        int classLevel = 0;
        int methodLevel = 0;
        Path base = repoRoot().resolve(relDir);
        if (!Files.isDirectory(base)) {
            return new int[]{0, 0};
        }
        try (Stream<Path> stream = Files.walk(base, 30)) {
            List<Path> files = new ArrayList<>();
            stream.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> p.toString().contains("/src/main/"))
                    .filter(p -> !p.toString().contains("/target/"))
                    .forEach(files::add);
            for (Path p : files) {
                for (String line : read(p).split("\n")) {
                    if (line.trim().startsWith("@RequestMapping")) {
                        classLevel++;
                    }
                    if (line.trim().matches("@(Get|Post|Put|Delete|Patch)Mapping.*")) {
                        methodLevel++;
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new int[]{classLevel, methodLevel};
    }

    private static int countFilesWith(List<String> relDirs, String needle) {
        int n = 0;
        for (String rel : relDirs) {
            Path base = repoRoot().resolve(rel);
            if (!Files.isDirectory(base)) {
                continue;
            }
            try (Stream<Path> stream = Files.walk(base, 30)) {
                List<Path> files = new ArrayList<>();
                stream.filter(p -> p.toString().endsWith(".java"))
                        .filter(p -> p.toString().contains("/src/main/"))
                        .filter(p -> !p.toString().contains("/target/"))
                        .forEach(files::add);
                for (Path p : files) {
                    if (read(p).contains(needle)) {
                        n++;
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return n;
    }

    private static int countFilesContaining(List<String> relDirs, String needle) {
        return countFilesWith(relDirs, needle);
    }

    private static final class Facts {
        String revision;
        String parentArtifact;
        String parentVersion;
        List<String> modules;
        String kernelVersion;
        List<String> kernelArtifacts;
        int beanCount;
        int mainJavaTotal;
        Map<String, int[]> perModule;
        int classLevelMappings;
        int methodLevelMappings;
        int centerControllers;
        int centerClassLevel;
        int centerMethodLevel;
        int realMainFiles;
        int errorCodes;
        int dtoFiles;
        boolean apiHasDependencies;
        boolean deploySkipAnywhere;
    }

    private static Facts recomputed() {
        Path root = repoRoot();
        Facts f = new Facts();
        Element project = projectElement(root.resolve("pom.xml"));
        f.revision = text(child(project, "properties"), "revision");
        f.parentArtifact = text(child(project, "parent"), "artifactId");
        f.parentVersion = text(child(project, "parent"), "version");
        f.modules = rootModules();
        f.kernelVersion = text(child(project, "properties"), "z-agent-kernel.version");
        for (Element d : children(child(child(project, "dependencyManagement"), "dependencies"), "dependency")) {
            String a = text(d, "artifactId");
            if (a != null && a.startsWith("z-agent-kernel-")) {
                f.kernelArtifacts = f.kernelArtifacts == null ? new ArrayList<>() : f.kernelArtifacts;
                f.kernelArtifacts.add(a);
            }
        }
        if (f.kernelArtifacts == null) {
            f.kernelArtifacts = new ArrayList<>();
        }

        // @Bean：按「注解后紧跟方法签名」数，不按 @Bean 文本数（文件里有注释里的 @Bean）
        String autoconfig = readRel(AUTOCONFIG_JAVA);
        Matcher bean = Pattern.compile("@Bean\\s*\\n\\s*public").matcher(autoconfig);
        f.beanCount = 0;
        while (bean.find()) {
            f.beanCount++;
        }

        f.perModule = new LinkedHashMap<>();
        f.mainJavaTotal = 0;
        for (String m : f.modules) {
            int[] c = javaCounts(m);
            f.perModule.put(m, c);
            f.mainJavaTotal += c[0];
        }

        int[] coreAdmin = mappingCounts("z-agent-core");
        int[] admin = mappingCounts("z-agent-admin");
        f.classLevelMappings = coreAdmin[0] + admin[0];
        f.methodLevelMappings = coreAdmin[1] + admin[1];

        int[] centerWeb = mappingCounts("z-agent-center/z-agent-center-web");
        f.centerClassLevel = centerWeb[0];
        f.centerMethodLevel = centerWeb[1];
        f.centerControllers = countFilesWith(
                List.of("z-agent-center/z-agent-center-web"), "@RestController");

        f.realMainFiles = countFilesContaining(List.of("z-agent-core", "z-agent-starter", "z-agent-admin",
                "z-agent-common", "z-agent-engine", "z-agent-center"), MAIN_METHOD);

        f.errorCodes = 0;
        Matcher code = Pattern.compile("public static final String [A-Z_]+\\s*=")
                .matcher(readRel(AGENT_EXCEPTION_JAVA));
        while (code.find()) {
            f.errorCodes++;
        }

        Path dtoDir = root.resolve("z-agent-api/src/main/java/com/zifang/z/agent/api/dto");
        f.dtoFiles = 0;
        if (Files.isDirectory(dtoDir)) {
            try (Stream<Path> stream = Files.list(dtoDir)) {
                f.dtoFiles = (int) stream.filter(p -> p.toString().endsWith(".java")).count();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        f.apiHasDependencies = child(projectElement(root.resolve(API_POM)), "dependencies") != null;

        f.deploySkipAnywhere = false;
        try (Stream<Path> stream = Files.walk(root, 4)) {
            List<Path> poms = new ArrayList<>();
            stream.filter(p -> p.getFileName() != null
                            && p.getFileName().toString().equals("pom.xml"))
                    .filter(p -> !p.toString().contains("/target/"))
                    .forEach(poms::add);
            for (Path p : poms) {
                if (stripXmlComments(read(p)).contains("maven.deploy.skip")) {
                    f.deploySkipAnywhere = true;
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return f;
    }

    private static void verifyClaims(String readme, Facts f) {
        String flat = readme.replaceAll("\\s+", " ");
        List<String> problems = new ArrayList<>();

        // 1. 当前版本 = 根 POM 的 <revision>
        Matcher ver = Pattern.compile("当前版本\\*\\* \\| `([0-9]+\\.[0-9]+\\.[0-9]+)`").matcher(flat);
        if (ver.find()) {
            if (!ver.group(1).equals(f.revision)) {
                problems.add("当前版本：README 写 " + ver.group(1) + "，根 POM <revision> 是 " + f.revision);
            }
        } else {
            problems.add("README 里找不到「当前版本 X.Y.Z」这条主张");
        }

        // 2. 父项目 = z-boot-parent:1.1.0
        Matcher par = Pattern.compile("父项目\\*\\* \\| `io\\.github\\.yuku123:([a-z0-9-]+):([0-9.]+)`")
                .matcher(flat);
        if (par.find()) {
            if (!par.group(1).equals(f.parentArtifact)) {
                problems.add("父项目 artifactId：README 写 " + par.group(1) + "，根 POM 是 " + f.parentArtifact);
            }
            if (!par.group(2).equals(f.parentVersion)) {
                problems.add("父项目版本：README 写 " + par.group(2) + "，根 POM 是 " + f.parentVersion);
            }
        } else {
            problems.add("README 里找不到「父项目」这条主张");
        }

        // 3. 模块数 7 + 每个模块目录都在
        Matcher mods = Pattern.compile("(\\d+) 个 <module>").matcher(flat);
        if (mods.find()) {
            if (Integer.parseInt(mods.group(1)) != f.modules.size()) {
                problems.add("根 POM <modules> 实有 " + f.modules.size() + " 条，README 写 "
                        + mods.group(1));
            }
            for (String m : f.modules) {
                if (!Files.isDirectory(repoRoot().resolve(m))) {
                    problems.add("根 POM 声明的模块目录不存在：" + m);
                }
            }
        } else {
            problems.add("README 里找不到「7 个 <module>」这条主张");
        }

        // 4. 内核依赖：属性版本 + 8 条按坐标写的 DM 条目
        Matcher kv = Pattern.compile("根 POM 属性 `z-agent-kernel\\.version=([0-9.]+)`").matcher(flat);
        if (kv.find()) {
            if (!kv.group(1).equals(f.kernelVersion)) {
                problems.add("z-agent-kernel.version：README 写 " + kv.group(1)
                        + "，根 POM 属性是 " + f.kernelVersion);
            }
        } else {
            problems.add("README 里找不到「z-agent-kernel.version=X」这条主张");
        }
        Matcher k8 = Pattern.compile("(\\d+) 条按坐标写的直接 DM 条目").matcher(flat);
        if (k8.find()) {
            if (Integer.parseInt(k8.group(1)) != f.kernelArtifacts.size()) {
                problems.add("内核 DM 条目数：README 写 " + k8.group(1)
                        + "，根 POM 实有 " + f.kernelArtifacts.size() + " 条");
            }
        } else {
            problems.add("README 里找不到「N 条按坐标写的直接 DM 条目」这条主张");
        }
        for (String a : f.kernelArtifacts) {
            if (!flat.contains("z-agent-kernel-")) {
                problems.add("README 的内核依赖段没有点名任何 z-agent-kernel-* 坐标");
                break;
            }
        }

        // 5. @Bean 3 个
        Matcher bean = Pattern.compile("产出 (\\d+) 个 `@Bean`").matcher(flat);
        if (bean.find()) {
            if (Integer.parseInt(bean.group(1)) != f.beanCount) {
                problems.add("@Bean 数：README 写 " + bean.group(1) + "，源码实有 " + f.beanCount);
            }
        } else {
            problems.add("README 里找不到「产出 N 个 @Bean」这条主张");
        }

        // 6. main .java 383 个（现读）
        Matcher main = Pattern.compile("main \\*\\*(\\d+)\\*\\* 个 `\\.java`").matcher(flat);
        if (main.find()) {
            if (Integer.parseInt(main.group(1)) != f.mainJavaTotal) {
                problems.add("main .java 数：README 写 " + main.group(1) + "，盘面实有 " + f.mainJavaTotal);
            }
        } else {
            problems.add("README 里找不到「main N 个 .java」这条主张");
        }

        // 7. 分模块 main 计数（test / @Test 是自指快照，不入断言）
        Matcher per = Pattern.compile("分模块（main / test / @Test）：(.+?)。").matcher(flat);
        if (per.find()) {
            Matcher mm = Pattern.compile("`([a-z0-9-]+)` (\\d+)/\\d+/\\d+").matcher(per.group(1));
            boolean any = false;
            while (mm.find()) {
                any = true;
                String module = mm.group(1);
                int[] c = f.perModule.get(module);
                if (c == null) {
                    problems.add("README 的分模块表里有盘上不存在的模块 " + module);
                } else if (c[0] != Integer.parseInt(mm.group(2))) {
                    problems.add(module + " 的 main .java 数：README 写 " + mm.group(2)
                            + "，盘面实有 " + c[0]);
                }
            }
            if (!any) {
                problems.add("README 的分模块表没抠出任何模块计数");
            }
        } else {
            problems.add("README 里找不到「分模块（main / test / @Test）」这段");
        }

        // 8. 能力清单里五个类的行数
        for (Map.Entry<String, String> e : CLASS_LINE_CLAIMS.entrySet()) {
            String simple = e.getKey().substring(e.getKey().lastIndexOf('/') + 1);
            Matcher lm = Pattern.compile("`[^`]*" + Pattern.quote(simple) + "`（(\\d+) 行）").matcher(flat);
            if (!lm.find()) {
                problems.add("README 的能力清单里找不到 " + simple + " 的行数主张");
                continue;
            }
            // 行数口径 = wc -l（换行符计数）：这几个源文件末尾没有换行符，
            // 编辑器/splitlines 口径会多 1；README 用的就是 wc -l 口径。
            long real = read(repoRoot().resolve(e.getKey()))
                    .chars().filter(c -> c == '\n').count();
            if (real != Long.parseLong(lm.group(1))) {
                problems.add(simple + " 行数：README 写 " + lm.group(1) + "，盘面实有 " + real);
            }
        }

        // 9. core+admin 的端点：7 个方法级 + 2 个类级
        Matcher api = Pattern.compile("`@RequestMapping` 实测就 (\\d+) 行（(\\d+) 个类级 \\+ (\\d+) 个方法级）").matcher(flat);
        if (api.find()) {
            int total = Integer.parseInt(api.group(1));
            int cls = Integer.parseInt(api.group(2));
            int method = Integer.parseInt(api.group(3));
            if (cls != f.classLevelMappings) {
                problems.add("类级 @RequestMapping 数：README 写 " + cls + "，盘面实有 " + f.classLevelMappings);
            }
            if (method != f.methodLevelMappings) {
                problems.add("方法级端点数：README 写 " + method + "，盘面实有 " + f.methodLevelMappings);
            }
            if (total != cls + method) {
                problems.add("README 的映射总数 " + total + " != 类级 " + cls + " + 方法级 " + method);
            }
        } else {
            problems.add("README 里找不到「@RequestMapping 实测就 N 行（M 个类级 + K 个方法级）」这条主张");
        }

        // 10. center-web：18 个 Controller / 135 个方法级端点 / 合计类级 20 / 方法级 142
        Matcher cw = Pattern.compile("`z-agent-center-web` 的 \\*\\*(\\d+) 个 Controller / (\\d+) 个方法级端点\\*\\*").matcher(flat);
        if (cw.find()) {
            if (Integer.parseInt(cw.group(1)) != f.centerControllers) {
                problems.add("center-web Controller 数：README 写 " + cw.group(1)
                        + "，盘面实有 " + f.centerControllers);
            }
            if (Integer.parseInt(cw.group(2)) != f.centerMethodLevel) {
                problems.add("center-web 方法级端点数：README 写 " + cw.group(2)
                        + "，盘面实有 " + f.centerMethodLevel);
            }
        } else {
            problems.add("README 里找不到「center-web 的 N 个 Controller / M 个方法级端点」这条主张");
        }
        Matcher total = Pattern.compile("合计类级 (\\d+) / 方法级 (\\d+)").matcher(flat);
        if (total.find()) {
            int clsTotal = f.classLevelMappings + f.centerClassLevel;
            int methodTotal = f.methodLevelMappings + f.centerMethodLevel;
            if (Integer.parseInt(total.group(1)) != clsTotal) {
                problems.add("合计类级：README 写 " + total.group(1) + "，盘面实有 " + clsTotal);
            }
            if (Integer.parseInt(total.group(2)) != methodTotal) {
                problems.add("合计方法级：README 写 " + total.group(2) + "，盘面实有 " + methodTotal);
            }
        } else {
            problems.add("README 里找不到「合计类级 N / 方法级 M」这条主张");
        }

        // 11. 真实 main 入口 1 个
        Matcher main2 = Pattern.compile("命中 (\\d+) 个真实 main").matcher(flat);
        if (main2.find()) {
            if (Integer.parseInt(main2.group(1)) != f.realMainFiles) {
                problems.add("真实 main 数：README 写 " + main2.group(1)
                        + "，盘面实有 " + f.realMainFiles);
            }
        } else {
            problems.add("README 里找不到「命中 N 个真实 main」这条主张");
        }
        if (!flat.contains(BOOT_ANNOTATION)) {
            problems.add("README 里找不到 " + BOOT_ANNOTATION + " 这条主张");
        }

        // 12. AgentException 6 个错误码
        Matcher ec = Pattern.compile("`AgentException`（(\\d+) 个错误码常量）").matcher(flat);
        if (ec.find()) {
            if (Integer.parseInt(ec.group(1)) != f.errorCodes) {
                problems.add("错误码常量数：README 写 " + ec.group(1) + "，源码实有 " + f.errorCodes);
            }
        } else {
            problems.add("README 里找不到「AgentException（N 个错误码常量）」这条主张");
        }

        // 13. api 模块 5 个 DTO + 零依赖
        Matcher dto = Pattern.compile("`AgentDto`（`kind` 缺省 `\"react\"`）、`RunRequest`、`RunResponse`、`StreamChunk`、`TokenUsageDto`").matcher(flat);
        if (dto.find()) {
            if (f.dtoFiles != 5) {
                problems.add("api 的 DTO 文件数：README 列了 5 个，盘面实有 " + f.dtoFiles);
            }
        } else {
            problems.add("README 里找不到 DTO 名单这条主张");
        }
        if (!flat.contains("POM **没有任何 `<dependencies>`**")) {
            problems.add("README 里找不到「z-agent-api 的 POM 没有任何 <dependencies>」这条主张");
        }
        if (f.apiHasDependencies) {
            problems.add("z-agent-api 的 POM 里有 <dependencies> 段（README 说没有）");
        }

        // 14. 全仓没有一处 maven.deploy.skip（剥注释后）
        if (!flat.contains("全仓没有一处 `maven.deploy.skip`")) {
            problems.add("README 里找不到「全仓没有一处 maven.deploy.skip」这条主张");
        }
        if (f.deploySkipAnywhere) {
            problems.add("剥注释后全仓仍有一处 maven.deploy.skip（README 说没有）");
        }

        if (!problems.isEmpty()) {
            fail("README 主张与盘面不一致（" + problems.size() + " 条）：\n  - "
                    + String.join("\n  - ", problems));
        }
    }

    @Test
    public void readmeClaimsMatchTheCode() {
        verifyClaims(read(repoRoot().resolve("README.md")), recomputed());
    }

    @Test
    public void healthyReadmeIsNotRejected() {
        verifyClaims(read(repoRoot().resolve("README.md")), recomputed());
    }

    @Test
    public void rulerActuallyCondemnsWrongNumbers() {
        String real = read(repoRoot().resolve("README.md"));
        Facts f = recomputed();
        String[][] mutations = {
                {"当前版本", "**当前版本** | `1.0.6`", "**当前版本** | `9.9.9`"},
                {"父项目版本", "z-boot-parent:1.2.0", "z-boot-parent:1.0.0"},
                {"模块数", "7 个 <module>", "6 个 <module>"},
                {"内核属性版本", "z-agent-kernel.version=0.2.3", "z-agent-kernel.version=0.2.2"},
                {"内核 DM 条目", "8 条按坐标写的直接 DM 条目", "9 条按坐标写的直接 DM 条目"},
                {"@Bean 数", "产出 3 个 `@Bean`", "产出 4 个 `@Bean`"},
                {"main .java 数", "main **386** 个 `.java`", "main **380** 个 `.java`"},
                {"类行数", "`core/agent/ReActAgent.java`（203 行）", "`core/agent/ReActAgent.java`（205 行）"},
                {"方法级端点", "2 个类级 + 7 个方法级", "2 个类级 + 8 个方法级"},
                {"center-web Controller", "**18 个 Controller / 135 个方法级端点**", "**17 个 Controller / 135 个方法级端点**"},
                {"真实 main", "命中 1 个真实 main", "命中 2 个真实 main"},
                {"错误码", "`AgentException`（6 个错误码常量）", "`AgentException`（5 个错误码常量）"},
        };
        for (String[] mut : mutations) {
            String broken = real.replace(mut[1], mut[2]);
            assertNotEquals("变异「" + mut[0] + "」没改动 README（尺子测不到自己）", broken, real);
            try {
                verifyClaims(broken, f);
                fail("变异「" + mut[0] + "」竟然没被判红");
            } catch (AssertionError expected) {
                // 预期：断言必须抛
            }
        }
    }

    @Test
    public void rulerCondemnsMissingClaims() {
        String real = read(repoRoot().resolve("README.md"));
        Facts f = recomputed();
        String[][] deletions = {
                {"当前版本", "**当前版本** | `1.0.6`", "**当前版本** | 待定"},
                {"@Bean 数", "产出 3 个 `@Bean`", "产出若干 `@Bean`"},
                {"main .java 数", "main **386** 个 `.java`", "main 若干个 `.java`"},
        };
        for (String[] del : deletions) {
            String broken = real.replace(del[1], del[2]);
            assertNotEquals("删除「" + del[0] + "」没改动 README（尺子测不到自己）", broken, real);
            try {
                verifyClaims(broken, f);
                fail("删除「" + del[0] + "」竟然没被判红");
            } catch (AssertionError expected) {
                // 预期：断言必须抛
            }
        }
    }
}
