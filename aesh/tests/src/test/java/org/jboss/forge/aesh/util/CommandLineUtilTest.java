/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.aesh.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import javax.inject.Inject;

import org.jboss.aesh.cl.CommandLine;
import org.jboss.aesh.cl.CommandLineParser;
import org.jboss.aesh.cl.OptionBuilder;
import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.cl.internal.ParameterInt;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.aesh.ForgeShell;
import org.jboss.forge.aesh.ShellContext;
import org.jboss.forge.arquillian.Addon;
import org.jboss.forge.arquillian.Dependencies;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.container.addons.AddonId;
import org.jboss.forge.container.addons.AddonRegistry;
import org.jboss.forge.container.repositories.AddonDependencyEntry;
import org.jboss.forge.ui.UICommand;
import org.jboss.forge.ui.context.UIBuilder;
import org.jboss.forge.ui.context.UIContext;
import org.jboss.forge.ui.context.UIValidationContext;
import org.jboss.forge.ui.impl.UIInputImpl;
import org.jboss.forge.ui.input.UIInput;
import org.jboss.forge.ui.metadata.UICommandMetadata;
import org.jboss.forge.ui.result.Result;
import org.jboss.forge.ui.util.Metadata;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@RunWith(Arquillian.class)
public class CommandLineUtilTest
{

   @Deployment
   @Dependencies({ @Addon(name = "org.jboss.forge:ui", version = "2.0.0-SNAPSHOT"),
            @Addon(name = "org.jboss.forge:aesh-test-harness", version = "2.0.0-SNAPSHOT"),
            @Addon(name = "org.jboss.forge:aesh", version = "2.0.0-SNAPSHOT"),
            @Addon(name = "org.jboss.forge:resources", version = "2.0.0-SNAPSHOT")
   })
   public static ForgeArchive getDeployment()
   {
      ForgeArchive archive = ShrinkWrap
               .create(ForgeArchive.class)
               .addBeansXML()
               .addAsAddonDependencies(
                        AddonDependencyEntry.create(AddonId.from("org.jboss.forge:ui", "2.0.0-SNAPSHOT")),
                        AddonDependencyEntry.create(AddonId.from("org.jboss.forge:aesh", "2.0.0-SNAPSHOT")),
                        AddonDependencyEntry.create(AddonId.from("org.jboss.forge:aesh-spi", "2.0.0-SNAPSHOT")),
                        AddonDependencyEntry.create(AddonId.from("org.jboss.forge:aesh-test-harness", "2.0.0-SNAPSHOT")),
                        AddonDependencyEntry.create(AddonId.from("org.jboss.forge:resources", "2.0.0-SNAPSHOT"))
               );

      return archive;
   }

   @Inject
   private ForgeShell shell;

   @Inject
   private AddonRegistry registry;

   @Test
   public void testGenerateParser() throws Exception
   {
      ShellContext context = new ShellContext(shell);
      Foo1Command foo1 = new Foo1Command();
      foo1.initializeUI(context);
      CommandLineParser parser = CommandLineUtil.generateParser(foo1, context);

      assertEquals("foo1", parser.getParameters().get(0).getName());

      Foo2Command foo2 = new Foo2Command();
      foo2.initializeUI(context);
      parser = CommandLineUtil.generateParser(foo2, context);

      ParameterInt param = parser.getParameters().get(0);
      assertEquals("foo2", param.getName());
      assertEquals("str", param.findLongOption("str").getLongName());
      assertEquals("bool", param.findLongOption("bool").getLongName());

   }

   @Test
   public void testPopulateUIInputs() throws CommandLineParserException
   {
      UIInput<String> input1 = new UIInputImpl<String>("str", String.class);
      UIInput<Integer> input2 = new UIInputImpl<Integer>("int", Integer.class);
      UIInput<Boolean> input3 = new UIInputImpl<Boolean>("bool", Boolean.class);

      ShellContext context = new ShellContext(shell);
      context.add(input1);
      context.add(input2);
      context.add(input3);

      ParameterInt param = new ParameterInt("test", "testing");
      param.addOption(new OptionBuilder().name('s').longName("str").description("yay").create());
      param.addOption(new OptionBuilder().name('i').longName("int").description("yay").create());
      param.addOption(new OptionBuilder().name('b').longName("bool").description("yay").hasValue(false).create());

      CommandLineParser clp = new CommandLineParser(param);
      CommandLine cl = clp.parse("test --str yay");

      CommandLineUtil.populateUIInputs(cl, context, registry);
      assertEquals(input1.getValue(), "yay");
      assertNull(input2.getValue());
      assertNull(input3.getValue());

      cl = clp.parse("test --str yay --int 10");
      CommandLineUtil.populateUIInputs(cl, context, registry);
      assertEquals(input1.getValue(), "yay");
      assertEquals(input2.getValue(), new Integer(10));
      assertNull(input3.getValue());

      cl = clp.parse("test --bool true");
      CommandLineUtil.populateUIInputs(cl, context, registry);
      assertEquals(Boolean.TRUE, input3.getValue());
      assertNull(input1.getValue());
      assertNull(input2.getValue());
   }

   @Test
   public void testGenerateAndPopulate() throws Exception
   {
      ShellContext context = new ShellContext(null);
      Foo2Command foo2 = new Foo2Command();
      foo2.initializeUI(context);
      CommandLineParser clp = CommandLineUtil.generateParser(foo2, context);

      CommandLine cl = clp.parse("foo2 --str yay");
      CommandLineUtil.populateUIInputs(cl, context, registry);

      assertEquals("yay", ((UIInput<?>) context.findInput("str")).getValue());
   }

   private class Foo1Command implements UICommand
   {
      @Override
      public UICommandMetadata getMetadata()
      {
         return Metadata.forCommand(getClass()).name("foo1").description("bla");
      }

      @Override
      public boolean isEnabled(UIContext context)
      {
         return true;
      }

      @Override
      public void initializeUI(UIBuilder builder) throws Exception
      {
      }

      @Override
      public void validate(UIValidationContext context)
      {
      }

      @Override
      public Result execute(UIContext context) throws Exception
      {
         return null;
      }
   }

   private class Foo2Command extends Foo1Command
   {
      private UIInput<String> str;
      private UIInput<Boolean> bool;

      @Override
      public UICommandMetadata getMetadata()
      {
         return Metadata.forCommand(getClass()).name("foo2").description("bla2");
      }

      @Override
      public void initializeUI(UIBuilder builder) throws Exception
      {
         str = new UIInputImpl<String>("str", String.class);
         bool = new UIInputImpl<Boolean>("bool", Boolean.class);

         builder.add(str).add(bool);
      }

   }
}
