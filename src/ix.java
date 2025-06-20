import java.io.IOException;
import java.util.Enumeration;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.ImageItem;
import javax.microedition.lcdui.ItemCommandListener;
import javax.microedition.lcdui.ItemStateListener;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;

public final class ix {
   public fz a;
   public eu a;
   public by a;
   public es a;
   public sdk.a a;
   public pf a;
   public String a = "";
   public String b = "";
   public String c = "";
   public String d = "";
   public String e = "";

   public final Displayable a(String var1, CommandListener var2) {
      Form var27;
      if (var1.equals("New Project")) {
         var27 = new Form("New Project");
         TextField var51 = new TextField("Project Name:", "MobileApplication", 32, 0);
         var27.append(var51);
         var27.append(new TextField("Project Location:", this.a.a, 128, 0));
         StringItem var48;
         (var48 = new StringItem("", "Browse...", 2)).setDefaultCommand(new Command("Browse", 8, 2));
         var48.setItemCommandListener((ItemCommandListener)var2);
         var27.append(var48);
         var27.append(new TextField("Project Folder:", this.a.a + "MobileApplication/", 128, 131072));
         var27.setItemStateListener((ItemStateListener)var2);
         var27.addCommand(new Command("Create", 4, 1));
         var27.addCommand(new Command("Cancel", 3, 2));
         var27.setCommandListener(var2);
         return var27;
      } else {
         Image var26;
         Image var37;
         Form var47;
         if (var1.equals("Choose File Type")) {
            var37 = null;
            var26 = null;

            try {
               var37 = Image.createImage("/img/java.png");
               var26 = Image.createImage("/img/package.png");
            } catch (Throwable var10) {
            }

            (var47 = new Form("New File")).append(new StringItem(var1, "", 0));
            ChoiceGroup var44;
            (var44 = new ChoiceGroup("File Types:", 1)).append("MIDlet", var37);
            var44.append("MIDP Canvas", var37);
            var44.append("Java Class", var37);
            var44.append("Java Interface", var37);
            var44.append("Java Package", var26);
            var47.append(var44);
            var47.addCommand(new Command("Next", 4, 1));
            var47.addCommand(new Command("Cancel", 3, 1));
            var47.setCommandListener(var2);
            return var47;
         } else {
            boolean var3;
            Image var7;
            if (var1.equals("New File")) {
               var3 = this.e.equals("Midlet");
               boolean var50 = this.e.equals("newpackage");
               (var47 = new Form(var1)).append(new StringItem("Name & Location", "", 0));
               if (var3) {
                  var47.append(new TextField("MIDlet Name:", this.e, 32, 0));
               }

               var47.append(new TextField((var50 ? "Package" : "Class") + " Name:", this.e, 32, 0));
               if (var3) {
                  var47.append(new TextField("MIDlet Icon:", "", 128, 0));
               }

               var47.append(new TextField("Project:", this.a.b, 32, 131072));
               if (!var50) {
                  String var42 = this.a.a + this.a.b + "/src/";
                  var7 = null;
                  String var40;
                  if (this.a.g.equals(var42)) {
                     var40 = "<default package>";
                  } else {
                     var40 = this.a.g.substring(var42.length(), this.a.g.length() - 1).replace('/', '.');
                  }

                  var47.append(new TextField("Package:", var40, 32, 131072));
               }

               var47.append(new TextField("Created " + (var50 ? "Folder:" : "File:"), this.a.g + this.e + (var50 ? "/" : ".java"), 128, 131072));
               if (var3) {
                  var47.append(new StringItem("", "Note: New MIDlets are automatically added to the application descriptor.", 0));
               }

               var47.setItemStateListener((ItemStateListener)var2);
               var47.addCommand(new Command("Finish", 4, 1));
               var47.addCommand(new Command("Back", 2, 1));
               var47.addCommand(new Command("Cancel", 3, 1));
               var47.setCommandListener(var2);
               return var47;
            } else {
               Image var6;
               Command var22;
               Image var28;
               if (var1.equals("Open Project")) {
                  var3 = this.b.equals(this.a.a);
                  var26 = null;
                  var28 = null;
                  var6 = null;

                  try {
                     var26 = Image.createImage("/img/drive.png");
                     var28 = Image.createImage("/img/up.png");
                     var6 = Image.createImage("/img/" + (var3 ? "project.png" : "folder.png"));
                  } catch (Throwable var13) {
                  }

                  List var38 = new List(var3 ? var1 : this.b, 3);
                  Enumeration var41;
                  if (this.b.equals("")) {
                     var38.setTitle(var1);

                     try {
                        var41 = pf.a();
                     } catch (Exception var12) {
                        var41 = null;
                     }
                  } else {
                     try {
                        this.a.a(this.b, this.a.b);
                        if (!this.a.a()) {
                           this.b = "";
                           var41 = pf.a();
                        } else {
                           var41 = this.a.a("*", true);
                           var38.append("..", var28);
                        }

                        this.a.a();
                     } catch (Exception var11) {
                        var41 = null;
                     }
                  }

                  if (var41 != null) {
                     while(var41.hasMoreElements()) {
                        String var39;
                        if ((var39 = "" + var41.nextElement()).endsWith("/")) {
                           var38.append(var39.substring(0, var39.length() - 1), this.b.equals("") ? var26 : var6);
                        }
                     }
                  }

                  var22 = new Command(var1, 4, 1);
                  var38.addCommand(var22);
                  var38.addCommand(new Command("Cancel", 3, 2));
                  var38.setCommandListener(var2);
                  var38.setSelectCommand(var22);
                  return var38;
               } else {
                  List var24;
                  if (var1.equals("Browser")) {
                     (var24 = this.a("Select Project Location", 3, false)).addCommand(new Command("Open", 4, 1));
                     var24.addCommand(new Command("Cancel", 3, 1));
                     var24.setCommandListener(var2);
                     return var24;
                  } else {
                     List var4;
                     Command var5;
                     if (!var1.equals("Add Resource") && !var1.equals("Add Resources")) {
                        Image var8;
                        List var9;
                        if (var1.equals("Projects")) {
                           var37 = null;
                           var26 = null;
                           var28 = null;
                           var6 = null;
                           var7 = null;
                           var8 = null;

                           try {
                              var37 = Image.createImage("/img/package.png");
                              var26 = Image.createImage("/img/srcpack.png");
                              var28 = Image.createImage("/img/res.png");
                              var6 = Image.createImage("/img/up.png");
                              var7 = Image.createImage("/img/java.png");
                              var8 = Image.createImage("/img/file.png");
                           } catch (Throwable var14) {
                           }

                           var9 = new List(this.a.b, 3);
                           var1 = this.a.a + this.a.b + "/src/";
                           if (!this.a.g.startsWith(var1)) {
                              var9.append("Source Packages", var26);
                              var9.append("Resources", var28);
                           } else {
                              if (this.a.g.equals(var1)) {
                                 var9.setTitle("<default package>");
                              } else {
                                 var9.setTitle(this.a.g.substring(var1.length()).replace('/', '.'));
                              }

                              var9.append("..", var6);

                              try {
                                 this.a.a(this.a.g, this.a.b);
                                 Enumeration var23 = this.a.a("*", true);
                                 this.a.a();
                                 int var20 = 1;

                                 while(var23.hasMoreElements()) {
                                    String var49;
                                    if ((var49 = "" + var23.nextElement()).endsWith("/")) {
                                       var9.insert(1, var49.replace('/', '.'), var37);
                                       ++var20;
                                    } else if (var49.endsWith(".java")) {
                                       var9.insert(var20, var49, var7);
                                    } else {
                                       var9.append(var49, var8);
                                    }
                                 }
                              } catch (Exception var19) {
                              }

                              var9.addCommand(new Command("New", 4, 2));
                              var9.addCommand(new Command("Add Resources...", 4, 3));
                              var9.addCommand(new Command("Delete", 4, 4));
                           }

                           Command var21 = new Command("Open", 4, 1);
                           var9.addCommand(var21);
                           var9.addCommand(new Command("Project Properties", 4, 5));
                           var9.addCommand(new Command("Menu", 2, 6));
                           var9.setCommandListener(var2);
                           var9.setSelectCommand(var21);
                           return var9;
                        } else if (var1.equals("Properties")) {
                           var37 = null;
                           var26 = null;
                           var28 = null;

                           try {
                              var37 = Image.createImage("/img/general.png");
                              var26 = Image.createImage("/img/appdes.png");
                              var28 = Image.createImage("/img/build.png");
                           } catch (Throwable var15) {
                           }

                           List var34;
                           (var34 = new List(var1, 3)).append("General", var37);
                           var34.append("Application Descriptor", var26);
                           var34.append("Build", var28);
                           var34.addCommand(new Command("OK", 4, 1));
                           var34.addCommand(new Command("Cancel", 3, 1));
                           var34.setCommandListener(var2);
                           return var34;
                        } else {
                           Form var32;
                           ChoiceGroup var43;
                           String[] var45;
                           if (var1.equals("General")) {
                              var45 = new String[]{"Automated Application Version Incrementation"};
                              (var32 = new Form(var1)).append(new TextField("Project Name:", this.a.b, 32, 131072));
                              var32.append(new TextField("Project Folder:", this.a.a + this.a.b, 128, 131072));
                              var32.append(new TextField("Project Sources Location:", "src", 32, 131072));
                              var32.append(new TextField("Application Version Number:", this.a.c, 8, 131072));
                              var32.append(new TextField("Application Version Counter:", Integer.toString(this.a.a), 7, 2));
                              (var43 = new ChoiceGroup("", 2, var45, (Image[])null)).setSelectedIndex(0, this.a.a);
                              var32.append(var43);
                              var32.addCommand(new Command("OK", 4, 1));
                              var32.addCommand(new Command("Cancel", 3, 1));
                              var32.setCommandListener(var2);
                              return var32;
                           } else {
                              Command var29;
                              if (!var1.equals("Application Descriptor") && !var1.equals("Build")) {
                                 if (!var1.equals("Attributes") && !var1.equals("MIDlets")) {
                                    if (!var1.equals("Add Attribute") && !var1.equals("Edit Attribute")) {
                                       if (!var1.equals("Add MIDlet") && !var1.equals("Edit MIDlet")) {
                                          String[] var46;
                                          if (var1.equals("Creating JAR")) {
                                             (var27 = new Form(var1)).append(new TextField("JAD File Name:", this.a.d, 32, 0));
                                             var27.append(new TextField("JAR File Name:", this.a.e, 32, 0));
                                             var46 = new String[]{"Compress JAR"};
                                             (var43 = new ChoiceGroup("", 2, var46, (Image[])null)).setSelectedIndex(0, this.a.b);
                                             var27.append(var43);
                                             var27.addCommand(new Command("OK", 4, 1));
                                             var27.addCommand(new Command("Cancel", 3, 1));
                                             var27.setCommandListener(var2);
                                             return var27;
                                          } else if (var1.equals("Compiling")) {
                                             var27 = new Form(var1);
                                             var46 = new String[]{"Generate Debugging Info", "Compile with Optimization"};
                                             (var43 = new ChoiceGroup("Javac Options:", 2, var46, (Image[])null)).setSelectedIndex(0, this.a.c);
                                             var43.setSelectedIndex(1, this.a.d);
                                             var27.append(var43);
                                             String[] var33 = new String[]{"ISO-8859-1", "UTF-8", "windows-1251"};
                                             ChoiceGroup var35;
                                             (var35 = new ChoiceGroup("Encoding:", 4, var33, (Image[])null)).setSelectedIndex(this.a.f.equals("ISO-8859-1") ? 0 : (this.a.f.equals("UTF-8") ? 1 : 2), true);
                                             var27.append(var35);
                                             var27.addCommand(new Command("OK", 4, 1));
                                             var27.addCommand(new Command("Cancel", 3, 1));
                                             var27.setCommandListener(var2);
                                             return var27;
                                          } else if (var1.equals("About")) {
                                             (var27 = new Form("About")).append("J2ME SDK Mobile v1.0\nCopyright © 2009-2010 by Mumey\ne-mail: mumey@list.ru\nwap: http://mumey.wen.ru/\n\nYou can donate project:\nR353295644791\nZ305686230796\nU724456730845\nE362870854655");
                                             var27.addCommand(new Command("Close", 2, 1));
                                             var27.setCommandListener(var2);
                                             return var27;
                                          } else if (var1.equals("Question")) {
                                             var37 = null;

                                             try {
                                                var37 = Image.createImage("/img/quest.png");
                                             } catch (Throwable var16) {
                                             }

                                             (var32 = new Form(var1)).append(new ImageItem("", var37, 3, ""));
                                             var32.append("\nFile " + this.a.a + this.a.c + " is modified. Save it ?");
                                             var32.addCommand(new Command("Save", 4, 1));
                                             var32.addCommand(new Command("Discard", 4, 2));
                                             var32.addCommand(new Command("Cancel", 3, 3));
                                             var32.setCommandListener(var2);
                                             return var32;
                                          } else if (var1.equals("Safe Delete")) {
                                             var27 = new Form(var1);
                                             if (this.e.endsWith("/")) {
                                                var27.append("Delete package " + this.e.substring(0, this.e.lastIndexOf(47)) + " and its contents.");
                                             } else {
                                                var27.append("Delete " + this.e.substring(0, this.e.lastIndexOf(46)));
                                             }

                                             var27.addCommand(new Command("OK", 4, 1));
                                             var27.addCommand(new Command("Cancel", 3, 1));
                                             var27.setCommandListener(var2);
                                             return var27;
                                          } else if (var1.equals("FindReplace")) {
                                             var27 = new Form("Find & Replace");
                                             var46 = new String[]{""};
                                             var27.append(new TextField("Find", this.a, 64, 0));
                                             var27.append(new ChoiceGroup("Case Sensitive", 2, var46, (Image[])null));
                                             var27.append(new ChoiceGroup("Replace", 2, var46, (Image[])null));
                                             var27.append(new TextField("Replace with", "", 64, 0));
                                             var5 = new Command("Go", 4, 1);
                                             var27.addCommand(var5);
                                             var27.addCommand(new Command("Back", 2, 1));
                                             var27.setCommandListener(var2);
                                             return var27;
                                          } else if (var1.equals("InsertCode")) {
                                             (var45 = this.a.a())[0] = "Add Code";
                                             var45[1] = "import ...";
                                             var4 = new List("Insert Code", 3, var45, (Image[])null);
                                             var5 = new Command("Select", 4, 1);
                                             var4.addCommand(var5);
                                             var4.addCommand(new Command("Back", 2, 1));
                                             var4.addCommand(new Command("Edit", 4, 2));
                                             var4.addCommand(new Command("Delete", 4, 3));
                                             var4.setCommandListener(var2);
                                             var4.setSelectCommand(var5);
                                             return var4;
                                          } else if (var1.equals("EditCode")) {
                                             (var27 = new Form((this.d.equals("") ? "Add" : "Edit") + " Code")).append(new TextField("Name", this.d, 25, 0));
                                             var27.append(new TextField("Code", this.a.a(this.d), 256, 0));
                                             var27.addCommand(new Command("Save", 4, 1));
                                             var27.addCommand(new Command("Back", 2, 1));
                                             var27.setCommandListener(var2);
                                             return var27;
                                          } else if (var1.equals("PackageBrowser")) {
                                             var24 = new List("", 3);
                                             var29 = new Command("Select", 4, 1);
                                             var24.addCommand(var29);
                                             var24.addCommand(new Command("Back", 2, 1));
                                             var24.setCommandListener(var2);
                                             var24.setSelectCommand(var29);
                                             return var24;
                                          } else if (var1.equals("Options")) {
                                             var37 = null;
                                             var26 = null;
                                             var28 = null;
                                             var6 = null;
                                             var7 = null;
                                             var8 = null;

                                             try {
                                                var37 = Image.createImage("/img/font.png");
                                                var26 = Image.createImage("/img/hotkeys.png");
                                                var28 = Image.createImage("/img/check.png");
                                                var6 = Image.createImage("/img/uncheck.png");
                                                var7 = Image.createImage("/img/scrsize.png");
                                                var8 = Image.createImage("/img/undo.png");
                                             } catch (IOException var17) {
                                             }

                                             (var9 = new List("Options", 3)).append("Text Color", this.a(this.a.j));
                                             var9.append("Background Color", a(this.a.a, this.a.b, this.a.c));
                                             var9.append("Caret Color", a(this.a.d, this.a.e, this.a.f));
                                             var9.append("Font", var37);
                                             var9.append("Hot Keys", var26);
                                             var9.append("Word Wrap", this.a.a ? var28 : var6);
                                             var9.append("Keep Indent", this.a.b ? var28 : var6);
                                             var9.append("Status Bar", this.a.a ? var28 : var6);
                                             var9.append("Screen Size", var7);
                                             var9.append("Undo Levels", var8);
                                             var22 = new Command("Select", 4, 1);
                                             var9.addCommand(var22);
                                             var9.addCommand(new Command("Back", 2, 1));
                                             var9.setCommandListener(var2);
                                             var9.setSelectCommand(var22);
                                             return var9;
                                          } else if (var1.equals("Screen Size")) {
                                             (var27 = new Form("Screen Size")).append(new TextField("Screen Width", this.a.q + "", 4, 2));
                                             var27.append(new TextField("Screen Height", this.a.r + "", 4, 2));
                                             var29 = new Command("Save", 4, 1);
                                             var27.addCommand(var29);
                                             var27.addCommand(new Command("Back", 2, 1));
                                             var27.setCommandListener(var2);
                                             return var27;
                                          } else if (!var1.equals("Background Color") && !var1.equals("Caret Color")) {
                                             ChoiceGroup var36;
                                             if (var1.equals("Font Choose")) {
                                                var27 = new Form("Choose a Font");
                                                (var36 = new ChoiceGroup("", 1)).append("Small", (Image)null);
                                                var36.append("Medium", (Image)null);
                                                var36.append("Large", (Image)null);
                                                if (this.a.g == 8) {
                                                   var36.setSelectedIndex(0, true);
                                                } else if (this.a.g == 0) {
                                                   var36.setSelectedIndex(1, true);
                                                } else if (this.a.g == 16) {
                                                   var36.setSelectedIndex(2, true);
                                                }

                                                var27.append(var36);
                                                var5 = new Command("Save", 4, 1);
                                                var27.addCommand(var5);
                                                var27.addCommand(new Command("Back", 2, 1));
                                                var27.setCommandListener(var2);
                                                return var27;
                                             } else if (var1.equals("Font Text")) {
                                                (var24 = new List("Choose Text Type", 3)).append("Regular Text", this.a(this.a.j));
                                                var24.append("Keywords", this.a(this.a.n));
                                                var24.append("Comments", this.a(this.a.l));
                                                var24.append("Strings", this.a(this.a.k));
                                                var24.append("Directives", this.a(this.a.p));
                                                var29 = new Command("Select", 4, 1);
                                                var24.addCommand(var29);
                                                var24.addCommand(new Command("Back", 2, 1));
                                                var24.setCommandListener(var2);
                                                var24.setSelectCommand(var29);
                                                return var24;
                                             } else if (var1.equals("Font Color")) {
                                                var27 = new Form("Choose Text Color");
                                                (var36 = new ChoiceGroup(this.c, 1)).append("Black", this.a(0));
                                                var36.append("Red", this.a(1));
                                                var36.append("Grey", this.a(2));
                                                var36.append("Blue", this.a(3));
                                                var36.append("Green", this.a(4));
                                                var36.append("Orange", this.a(5));
                                                if (this.c.toLowerCase().equals("regular text")) {
                                                   var36.setSelectedIndex(this.a.j, true);
                                                } else if (this.c.toLowerCase().equals("keywords")) {
                                                   var36.setSelectedIndex(this.a.n, true);
                                                } else if (this.c.toLowerCase().equals("strings")) {
                                                   var36.setSelectedIndex(this.a.k, true);
                                                } else if (this.c.toLowerCase().equals("comments")) {
                                                   var36.setSelectedIndex(this.a.l, true);
                                                } else if (this.c.toLowerCase().equals("braces")) {
                                                   var36.setSelectedIndex(this.a.m, true);
                                                } else if (this.c.toLowerCase().equals("tags")) {
                                                   var36.setSelectedIndex(this.a.o, true);
                                                } else if (this.c.toLowerCase().equals("directives")) {
                                                   var36.setSelectedIndex(this.a.p, true);
                                                }

                                                var27.append(var36);
                                                var5 = new Command("Save", 4, 1);
                                                var27.addCommand(var5);
                                                var27.addCommand(new Command("Back", 2, 1));
                                                var27.setCommandListener(var2);
                                                return var27;
                                             } else if (var1.equals("KB Map")) {
                                                var24 = new List("Hot Keys", 3, this.a.a(), (Image[])null);
                                                var29 = new Command("Select", 4, 1);
                                                var24.addCommand(var29);
                                                var5 = new Command("Save", 4, 2);
                                                var24.addCommand(var5);
                                                var24.addCommand(new Command("Back", 2, 1));
                                                var24.setCommandListener(var2);
                                                var24.setSelectCommand(var29);
                                                return var24;
                                             } else if (var1.equals("Undo Levels")) {
                                                (var27 = new Form("Undo Levels")).append(new TextField("Levels", "" + this.a.s, 3, 2));
                                                var29 = new Command("Save", 4, 1);
                                                var27.addCommand(var29);
                                                var27.addCommand(new Command("Back", 2, 1));
                                                var27.setCommandListener(var2);
                                                return var27;
                                             } else if (var1.equals("Output")) {
                                                (var27 = new Form(var1 + " - " + this.a.b + " (build)")).append("pre-init:\n");
                                                return var27;
                                             } else {
                                                (var27 = new Form("Error")).append("Error while loading screen: " + var1);
                                                return var27;
                                             }
                                          } else {
                                             if (var1.equals("Background Color")) {
                                                (var27 = new Form("Background Color")).append(new TextField("Red", this.a.a + "", 3, 2));
                                                var27.append(new TextField("Green", this.a.b + "", 3, 2));
                                                var27.append(new TextField("Blue", this.a.c + "", 3, 2));
                                             } else {
                                                (var27 = new Form("Caret Color")).append(new TextField("Red", this.a.d + "", 3, 2));
                                                var27.append(new TextField("Green", this.a.e + "", 3, 2));
                                                var27.append(new TextField("Blue", this.a.f + "", 3, 2));
                                             }

                                             var29 = new Command("Save", 4, 1);
                                             var27.addCommand(var29);
                                             var27.addCommand(new Command("Back", 2, 1));
                                             var27.setCommandListener(var2);
                                             return var27;
                                          }
                                       } else {
                                          if (var1.equals("Add MIDlet")) {
                                             this.a = "";
                                             this.c = "";
                                             this.e = "";
                                          }

                                          (var27 = new Form(var1)).append(new TextField("MIDlet Name:", this.a, 32, 0));
                                          var27.append(new TextField("MIDlet Class:", this.c, 128, 0));
                                          var27.append(new TextField("MIDlet Icon:", this.e, 128, 0));
                                          var27.addCommand(new Command("OK", 4, 1));
                                          var27.addCommand(new Command("Cancel", 3, 1));
                                          var27.setCommandListener(var2);
                                          return var27;
                                       }
                                    } else {
                                       int var25 = 131072;
                                       if (var1.equals("Add Attribute")) {
                                          this.a = "";
                                          this.c = "";
                                          var25 = 0;
                                       }

                                       (var32 = new Form(var1)).append(new TextField("Name:", this.a, 128, var25));
                                       var32.append(new TextField("Value:", this.c, 255, 0));
                                       var32.append(new TextField("Placement:", "JAD and Manifest", 16, 131072));
                                       var32.addCommand(new Command("OK", 4, 1));
                                       var32.addCommand(new Command("Cancel", 3, 1));
                                       var32.setCommandListener(var2);
                                       return var32;
                                    }
                                 } else {
                                    var24 = new List(var1, 3);
                                    int var30;
                                    String var31;
                                    if (var1.equals("MIDlets")) {
                                       for(var30 = 0; var30 < this.a.a.size(); ++var30) {
                                          var31 = "MIDlet-" + Integer.toString(var30 + 1) + ": " + (String)this.a.a.elementAt(var30);
                                          var24.append(var31, (Image)null);
                                       }
                                    } else {
                                       for(var30 = 0; var30 < this.a.b.size(); ++var30) {
                                          var31 = (String)this.a.b.elementAt(var30);
                                          var24.append(var31, (Image)null);
                                       }
                                    }

                                    var24.addCommand(new Command("Add...", 4, 1));
                                    var29 = new Command("Edit...", 4, 2);
                                    var24.addCommand(new Command("Remove", 4, 3));
                                    var24.addCommand(var29);
                                    var24.addCommand(new Command("Back", 2, 4));
                                    var24.setCommandListener(var2);
                                    var24.setSelectCommand(var29);
                                    return var24;
                                 }
                              } else {
                                 var24 = new List(var1, 3);
                                 if (var1.equals("Build")) {
                                    var26 = null;
                                    var28 = null;

                                    try {
                                       var26 = Image.createImage("/img/build.png");
                                       var28 = Image.createImage("/img/jar.png");
                                    } catch (Throwable var18) {
                                    }

                                    var24.append("Compiling", var26);
                                    var24.append("Creating JAR", var28);
                                 } else {
                                    var24.append("Attributes", (Image)null);
                                    var24.append("MIDlets", (Image)null);
                                 }

                                 var29 = new Command("Select", 4, 1);
                                 var24.addCommand(var29);
                                 var24.addCommand(new Command("Back", 2, 1));
                                 var24.setCommandListener(var2);
                                 var24.setSelectCommand(var29);
                                 return var24;
                              }
                           }
                        }
                     } else {
                        var3 = var1.equals("Add Resources");
                        var4 = this.a(var1, var3 ? 2 : 3, true);
                        var5 = new Command("Add", 4, 1);
                        var4.addCommand(var5);
                        if (!var3) {
                           var4.addCommand(new Command("Mark several", 4, 2));
                        }

                        var4.addCommand(new Command("Back", 2, 3));
                        var4.setCommandListener(var2);
                        if (!var3) {
                           var4.setSelectCommand(var5);
                        }

                        return var4;
                     }
                  }
               }
            }
         }
      }
   }

   private List a(String var1, int var2, boolean var3) {
      Image var4 = null;
      Image var5 = null;
      Image var6 = null;
      Image var7 = null;
      Image var8 = null;

      try {
         var4 = Image.createImage("/img/drive.png");
         var5 = Image.createImage("/img/up.png");
         var6 = Image.createImage("/img/folder.png");
         if (var3) {
            var7 = Image.createImage("/img/java.png");
            var8 = Image.createImage("/img/file.png");
         }
      } catch (Throwable var12) {
      }

      List var9 = new List(this.b, var2);
      Enumeration var13;
      if (this.b.equals("")) {
         var9.setTitle(var1);

         try {
            var13 = pf.a();
         } catch (Exception var11) {
            var13 = null;
         }
      } else {
         try {
            this.a.a(this.b, this.a.b);
            var13 = this.a.a("*", true);
            this.a.a();
            if (var2 == 3) {
               var9.append("..", var5);
            }
         } catch (Exception var10) {
            var13 = null;
         }
      }

      if (var13 != null) {
         while(var13.hasMoreElements()) {
            String var14;
            if ((var14 = "" + var13.nextElement()).endsWith("/")) {
               if (var2 == 3) {
                  var9.append(var14, this.b.equals("") ? var4 : var6);
               }
            } else if (var3) {
               if (var14.endsWith(".java")) {
                  var9.append(var14, var7);
               } else {
                  var9.append(var14, var8);
               }
            }
         }
      }

      return var9;
   }

   private static Image a(int var0, int var1, int var2) {
      Image var3;
      Graphics var4;
      (var4 = (var3 = Image.createImage(15, 15)).getGraphics()).setColor(var0, var1, var2);
      var4.fillRect(0, 0, 15, 15);
      var4.setColor(0, 0, 0);
      var4.drawRect(0, 0, 14, 14);
      return var3;
   }

   private Image a(int var1) {
      short var2;
      short var3;
      short var4;
      switch(var1) {
      case 0:
         var3 = 0;
         var4 = 0;
         var2 = 0;
         break;
      case 1:
         var3 = 255;
         var4 = 0;
         var2 = 0;
         break;
      case 2:
         var3 = 150;
         var4 = 150;
         var2 = 150;
         break;
      case 3:
         var3 = 0;
         var4 = 0;
         var2 = 230;
         break;
      case 4:
         var3 = 0;
         var4 = 112;
         var2 = 0;
         break;
      case 5:
         var3 = 206;
         var4 = 123;
         var2 = 0;
         break;
      default:
         return null;
      }

      return a(var3, var4, var2);
   }
}
