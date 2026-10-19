import sys, os

BS = chr(92)
qa = os.path.abspath('qa').replace('/', BS)
SETUP = ("persp|org.eclipse.jdt.ui.JavaPerspective;view|org.eclipse.jdt.ui.PackageExplorer;project;"
         "view|org.eclipse.ui.console.ConsoleView;view|org.eclipse.ui.views.ProblemView;view|spike.p8.view;"
         "wait|2500;state|start;")
FILES = {'atom': 'R8A QA - Atom One Dark', 'dracula': 'R8A QA - Dracula', 'mocha': 'R8A QA - Catppuccin Mocha',
         'gruvbox': 'R8A QA - Gruvbox Dark', 'solarized': 'R8A QA - Solarized Light',
         'github': 'R8A QA - GitHub Light', 'teal': 'R8A QA - Unusual Teal'}


def imports(keys):
    return ''.join('import|%s%s%s.xml;' % (qa, BS, k) for k in dict.fromkeys(keys))


def seq(keys, reopen=False):
    s = 'prefs;wait|1500;'
    for i, k in enumerate(keys):
        s += 'pick|%s;' % FILES[k]
        if i == 0:
            s += 'check|1;'
        s += 'apply;wait|4500;state|%s;regs|%s;eprefs|%s;' % (k, k, k)
        s += 'screen|%s-dlg|dialog;audit|%s-dlg|dialog;wait|1200;' % (k, k)
        s += 'pcancel;wait|1500;'
        s += 'screen|%s;audit|%s;tabs|%s;wait|1200;' % (k, k, k)
        s += 'prefs;wait|1500;'
        if reopen:
            s += 'screen|%s-reopened-dlg|dialog;audit|%s-reopened|dialog;wait|1200;' % (k, k)
    s += 'okclose;wait|1500;state|end;'
    return s


if __name__ == '__main__':
    name = sys.argv[1]
    keys = sys.argv[2].split(',')
    extra = sys.argv[3] if len(sys.argv) > 3 else ''
    setup = SETUP.replace('state|start;', 'stock|org.eclipse.e4.ui.css.theme.e4_dark;wait|4000;state|start;') if 'stockdark' in extra else SETUP
    open(name + '.txt', 'w').write(setup + imports(keys) + seq(keys, reopen='reopen' in extra))
