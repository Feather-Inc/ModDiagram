package com.juanmuscaria.mod_diagram;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.versioning.ArtifactVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.*;

@Mod(modid = Tags.MODID, version = Tags.VERSION, name = Tags.MODNAME, acceptedMinecraftVersions = "[1.7.10]")
public class ModDiagram {

  public static final Logger LOG = LogManager.getLogger(Tags.MODID);
  // Eh? I'm not going to write a fully compliant mermaid api just for that
  public static final String MOD_TEMPLATE = """
            class `%s`["%s"] {
              id: %s
              version: %s
              authors: %s
            }
          """;
  private static final List<String> idBlacklist = Arrays.asList("Forge", "mcp", "FML");

  @EventHandler
  public void postInit(FMLPostInitializationEvent event) {
    LOG.info("Generating mod mermaid diagram and mod list...");
    var graph = new StringBuilder("```mermaid\nclassDiagram\n");
    var list = new StringBuilder("# Mods ");
    var mods = new ArrayList<>(Loader.instance().getModList());
    mods.sort(Comparator.comparing(ModContainer::getModId));

    list.append('(').append(mods.size()).append(")\n");

    for (ModContainer mod : mods) {
      // Skip forge mod containers
      if (shouldSkip(mod.getModId())) {
        continue;
      }

      graph.append(String.format(MOD_TEMPLATE, sanitize(mod.getModId()), mod.getName(), mod.getModId(),
              mod.getDisplayVersion(), String.join(", ", mod.getMetadata().authorList)));
      for (ArtifactVersion dependency : mod.getDependencies()) {
        if (shouldSkip(dependency.getLabel()) || !Loader.instance().getIndexedModList().containsKey(dependency.getLabel())) {
          continue; // Ignore dependencies not loaded
        }
        graph.append(String.format("  `%s` <-- `%s`\n", sanitize(mod.getModId()), sanitize(dependency.getLabel())));
      }

      graph.append("\n");

      list.append("* ").append(mod.getName()).append("-\\[").append(mod.getModId().replace("<","\\<")).append("]-")
              .append(mod.getVersion()).append('\n');
    }
    graph.append("```");
    try {
      Files.write(FileSystems.getDefault().getPath("./mod-diagram.md"),
              graph.toString().getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      LOG.error("Unable to write down mod diagram", e);
    }

    try {
      Files.write(FileSystems.getDefault().getPath("./mod-list.md"),
              list.toString().getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      LOG.error("Unable to write down mod list", e);
    }
  }

  private boolean shouldSkip(String modId) {
    return idBlacklist.contains(modId);
  }

  private String sanitize(String original) {
    return original.replace('`', '\'');
  }
}
