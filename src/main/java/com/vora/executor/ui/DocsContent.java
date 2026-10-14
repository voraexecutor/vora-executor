package com.vora.executor.ui;

public final class DocsContent {

    public static final String HTML = """
<html><body>

<h1>Vora - Java Executor for Minecraft</h1>
<p>A complete guide to writing Java code that runs inside a live Minecraft client.</p>

<hr>

<h2>Quick Start</h2>
<ol>
<li>Start Minecraft and enter a world (some scripts need a world, some don't).</li>
<li>Open Vora and select the Minecraft process from the dropdown at the top.</li>
<li>Write your code in the editor.</li>
<li>Pick the right file tab - <b>mixin.java</b> for tick hooks, <b>script.java</b> for one-shot code.</li>
<li>Press <b>EXECUTE</b>.</li>
<li>Read the console at the bottom for output and errors.</li>
<li>Edit and run again - no restart needed between runs.</li>
</ol>
<p>The left sidebar toggles the editor, console, documentation, and scripts history. The last button,
<b>Scripts</b>, opens the history of everything you've run.</p>

<h2>Scripts History</h2>
<p>Every script you run is added to the <b>Scripts</b> list (newest first). Each entry is saved
to disk so the list survives restarts, showing the time, run mode (mixin/code), and a short ID.</p>
<ul>
<li><b>Open</b> - puts that script back into the editor so you can tweak and re-run it.</li>
<li><b>Rename</b> - gives the script a custom name that persists across restarts.</li>
<li><b>Remove</b> - deletes the script (asks for confirmation if it's a favorite).</li>
<li><b>Clear history</b> - removes every script except favorites.</li>
</ul>

<h2>Run as Mixin or Code</h2>
<p>The editor has two file tabs. Which one is open when you press <b>EXECUTE</b> determines how Vora runs it:</p>

<h3>mixin.java - Tick Hooks (Runs Every Tick)</h3>
<p>This file is hooked into <code>MinecraftClient.tick</code> and executes on every game tick
while Vora stays attached (~20 times/second). Use it for anything that needs to run continuously:</p>
<ul>
<li>Key polling and auto-features (auto-sprint, auto-eat, auto-tool)</li>
<li>HUD overlays and rendering</li>
<li>Periodic world/block/entity checks</li>
<li>State machines that update per-tick</li>
</ul>
<p style="color: {{MUTED}};"><b>Mixin rules:</b> Your mixin body runs ~20x/second inside the tick loop.
Keep it cheap - never <code>Thread.sleep</code>, loop forever, or open a window from mixin mode,
or the game will freeze. Anything that should happen once must be gated by game state
(e.g. <code>mc.world.getTime() % 20 == 0</code>) rather than a local counter (which resets on
every recompile).</p>

<h3>script.java - One-Shot Execution (Runs Once)</h3>
<p>This file runs exactly once on the client thread and finishes. Use it for discrete actions:</p>
<ul>
<li>Opening a screen or sending a chat message</li>
<li>Spawning entities, placing blocks, attacking</li>
<li>One-time inventory manipulation</li>
<li>Quick diagnostics and dumps</li>
</ul>
<p>Both files keep their own text - switch between them any time and run each as often as you like.
The active file tab is marked with a colored underline.</p>

<h3>A Working mixin.java Example</h3>
<p>Body only - runs every tick. Guards against being outside a world and only logs once per second:</p>
<table width="100%" cellspacing="0" cellpadding="10" border="0" bgcolor="{{CODE_BG}}">
<tr><td><pre>{{MIXIN_EXAMPLE}}</pre></td></tr>
</table>

<h3>A Working script.java Example</h3>
<p>Sends a chat message:</p>
<table width="100%" cellspacing="0" cellpadding="10" border="0" bgcolor="{{CODE_BG}}">
<tr><td><pre>mc.player.sendMessage(net.minecraft.text.Text.literal("Hello from Vora!"), false);</pre></td></tr>
</table>

<h2>How a Script Is Wrapped</h2>
<p>You write only the <b>body</b> of a method. Vora wraps it into a small class before compiling.
Conceptually, this is what gets built:</p>
<table width="100%" cellspacing="0" cellpadding="10" border="0" bgcolor="{{CODE_BG}}">
<tr><td><pre>package org.client.executor.script;

import net.minecraft.client.MinecraftClient;
import java.util.*;
import ...your imports...

public class Script {
    public static void log(String message) { ... }

    private void body() throws Throwable {
        MinecraftClient mc = MinecraftClient.getInstance();
        // YOUR CODE HERE
    }
}</pre></td></tr>
</table>
<p>Because of this, <code>mc</code> is already in scope, <code>log(...)</code> is available,
and you may use <code>return;</code> to stop early. You cannot add new methods to the class,
but you can use local variables, lambdas, and local classes inside your body.</p>

<h2>The Rules of a Script</h2>
<ul>
<li>No class, no <code>main</code>, no package. Just the method body.</li>
<li><b>mc</b> is predefined and is the live <code>MinecraftClient</code>.</li>
<li><b>log(...)</b> prints any value to the Vora console.</li>
<li><b>import ...;</b> lines are allowed anywhere; Vora moves them to the top.</li>
<li>Exceptions are caught automatically and printed in red with stack traces.</li>
<li><code>return;</code> stops the script early.</li>
<li>The whole body runs on the client thread - safe to read and change game state.</li>
<li>A script must finish within 120 seconds (timeout).</li>
<li>You can only use classes that already exist in the game process.</li>
</ul>

<h2>The <code>mc</code> Object - Your Gateway to the Game</h2>
<p>Predefined as <code>MinecraftClient mc = MinecraftClient.getInstance()</code>.</p>

<h3>Common Fields</h3>
<table width="100%" cellspacing="0" cellpadding="10" border="0" bgcolor="{{CODE_BG}}">
<tr><td><pre>mc.player              // Your player (PlayerEntity) - null in menus
mc.world               // Current world (ClientWorld) - null in menus
mc.options             // Game options (Keybinds, settings)
mc.getWindow()         // Window handle (for GLFW keys, cursor)
mc.interactionManager  // For attacking, interacting, block placement
mc.textRenderer        // Font renderer (TextRenderer)
mc.player.getHealth()  // Current health (float)
mc.player.getPos()     // Position (Vec3d)
mc.player.getInventory() // PlayerInventory
mc.player.getAbilities() // Creative, flying, etc. (PlayerAbilities)
mc.player.getMainHandStack() // ItemStack in main hand</pre></td></tr>
</table>

<h3>Useful Methods</h3>
<table width="100%" cellspacing="0" cellpadding="10" border="0" bgcolor="{{CODE_BG}}">
<tr><td><pre>mc.execute(runnable)     // Run on client thread (REQUIRED for GUI)
mc.setScreen(screen)      // Open a screen
mc.player.sendMessage(Text.literal("msg"), false) // Send chat
mc.player.attack(target)  // Attack entity
mc.interactionManager.interactBlock(player, hand, hitResult) // Place block
mc.player.getInventory().insertStack(stack) // Give item
mc.player.getInventory().selectedSlot = n   // Switch hotbar slot
mc.options.keySprint.setPressed(true)       // Simulate key press (use KeyBindingHelper)
mc.doAttack()             // Attack once
mc.crosshairTarget        // What the player is looking at (HitResult)</pre></td></tr>
</table>

<h3>World and Entity Queries</h3>
<table width="100%" cellspacing="0" cellpadding="10" border="0" bgcolor="{{CODE_BG}}">
<tr><td><pre>mc.world.getPlayers()                 // List of nearby players
mc.world.getEntities()                 // All loaded entities
mc.world.getBlockState(pos)            // BlockState at BlockPos
mc.world.getBlockEntity(pos)           // BlockEntity at BlockPos
mc.world.raycast(context)              // Raycast from camera
mc.world.getEntitiesByType(type, box, predicate) // Filtered entity search</pre></td></tr>
</table>

<h2>Logging</h2>
<p>Use <code>log(...)</code> to print to the Vora console (any type works):</p>
<table width="100%" cellspacing="0" cellpadding="10" border="0" bgcolor="{{CODE_BG}}">
<tr><td><pre>log("Hello " + mc.player.getName().getString());
log("Health: " + mc.player.getHealth());
log("Pos: " + mc.player.getPos());
log("Inventory: " + mc.player.getInventory().main.size());
log(mc.player.getInventory().getStack(0));</pre></td></tr>
</table>

<h2>Errors and the Console</h2>
<ul>
<li>Compilation errors show in red with line numbers mapped to your source.</li>
<li>Runtime exceptions print full stack traces in red.</li>
<li>Check the console tab for output - it timestamps every line.</li>
<li>Script times out after 120 seconds (watchdog).</li>
</ul>

<h2>Imports and What You Can Use</h2>
<ul>
<li>Any class from <b>Minecraft</b> (<code>net.minecraft.*</code>)</li>
<li>Any class from <b>Java standard library</b> (<code>java.util</code>, <code>java.lang</code>, <code>java.io</code>, etc.)</li>
<li>Any class from <b>Mapped libraries</b> (LWJGL, Text components, etc.)</li>
<li>Write <code>import net.minecraft.client.gui.screen.Screen;</code> anywhere - Vora moves them to the top</li>
</ul>
<p style="color: {{MUTED}};">On versions without Yarn mappings (pre-1.20.5 or no Yarn build), Vora falls back
to Mojang's official mappings. Class names stay the same (<code>MinecraftClient</code>,
<code>DrawContext</code>, <code>Text</code>), but method/field names may differ slightly
(e.g., <code>getInstance()</code> vs <code>getInstance()</code> - usually identical).</p>

<h2>How to Find the Right Names</h2>
<p>Minecraft ships obfuscated. Inside a production client the real names look like
<code>fgo</code> or <code>method_5477</code>. Two naming layers are involved:</p>
<ul>
<li><b>Intermediary</b> - stable runtime names such as <code>net.minecraft.class_310</code>.</li>
<li><b>Yarn</b> - readable names such as <code>MinecraftClient</code>.</li>
</ul>
<p>You always write <b>Yarn</b> names. Vora remaps your compiled code from Yarn into intermediary
so the game can load it, including methods inherited from parent classes. That's why
<code>player.getName()</code> works even though the method is defined on <code>Entity</code>,
not the player class.</p>

<h2>First Run and Caching</h2>
<p>The first time you run a script for a given game version, Vora downloads the mapping
files and builds a named copy of the game jar. This needs an internet connection on the
game machine and takes a few seconds. Everything is cached afterwards in
<code>%TEMP%\\vora\\</code>, so later runs start almost instantly.</p>

<h2>Limits and Warnings</h2>
<ul>
<li><b>Client side only.</b> The code runs inside the Minecraft client, not on a server.</li>
<li><b>Classpath limited.</b> You can only use classes already present in the game.</li>
<li><b>120 second timeout.</b> A script must finish within 120 seconds.</li>
<li><b>Don't block the client thread.</b> Long computations freeze the game.</li>
<li><b>Anti-cheat / server visibility.</b> You are running as the player - anything you do
is visible to the server and to anti-cheat. Use at your own risk.</li>
<li><b>Mixin re-runs on every EXECUTE.</b> Local variables don't persist across presses.</li>
</ul>

<h2>Troubleshooting</h2>
<h3>"Agent JAR loaded but agent failed to initialize"</h3>
<p>Means the agent couldn't start. Check <code>%USERPROFILE%\\.vora\\agent\\agent-error.log</code>
for the real cause. Common: Java version mismatch (fixed in recent builds - ensure Java 17+).</p>

<h3>"The import net.minecraft.client.gui.DrawContext cannot be resolved"</h3>
<p>Fixed in recent builds - the wrapper no longer assumes 1.20+ APIs. Update your jar.</p>

<h3>Script works in script.java but not mixin.java (or vice versa)</h3>
<p>Check the run mode tab. Mixin runs every tick; script runs once. GUI code needs <code>mc.execute</code>.</p>

<h3>Mapping download fails</h3>
<p>First run needs internet. If it fails, check firewall/antivirus. Cached mappings are in
<code>%TEMP%\\vora\\mappings\\</code> - you can copy them from another machine.</p>

<hr>
<p>Vora - Java executor for Minecraft | <a href="https://discord.gg/Nm2K5Mk72M">Discord</a></p>

</body></html>
""";
}