"""Exercise the production shell logic only in a temporary host namespace."""
from pathlib import Path
import os, subprocess, tempfile

root = Path(__file__).resolve().parents[1]
with tempfile.TemporaryDirectory(prefix='nubialy-guard-test-') as work:
    work = Path(work)
    module = work / 'module'
    module.mkdir()
    guard = work / 'guard'
    for source in (root / 'recovery-module').glob('*.sh'):
        text = source.read_text().replace('guard_dir=/data/local/tmp/nubia-ly-guard', f'guard_dir={guard}')
        (module / source.name).write_text(text)
    binaries = work / 'bin'
    binaries.mkdir()
    (binaries / 'getprop').write_text('#!/bin/sh\ncase "$1" in ro.product.model) echo "${TEST_MODEL:-NX809J}" ;; ro.build.version.sdk) echo 36 ;; esac\n')
    (binaries / 'id').write_text('#!/bin/sh\necho "${TEST_UID:-0}"\n')
    for executable in binaries.iterdir(): executable.chmod(0o755)
    environment = dict(os.environ, PATH=f'{binaries}:{os.environ["PATH"]}')
    def run(command, success=True, extra=None, script='guardctl.sh'):
        result = subprocess.run(['sh', str(module / script), command], env=dict(environment, **(extra or {})), capture_output=True, text=True)
        assert (result.returncode == 0) == success, (command, result.stdout, result.stderr)
        return result
    run('status')
    assert not guard.exists()
    run('probe', False, {'TEST_MODEL':'other'})
    assert not guard.exists()
    run('probe', False, {'TEST_UID':'2000'})
    assert not guard.exists()
    run('probe')
    assert guard.stat().st_mode & 0o777 == 0o755
    assert (guard / 'probe').stat().st_mode & 0o777 == 0o644
    lines = (guard / 'probe').read_text().splitlines()
    assert lines[0] == 'nubia-ly-probe-v1' and lines[3] == 'probe-only'
    assert 0 < int(lines[2]) - int(float(Path('/proc/uptime').read_text().split()[0])) <= 60
    run('preview')
    run('clock')
    color_lines = (guard / 'clock').read_text().splitlines()
    assert color_lines[0] == 'nubia-ly-clock-v1' and color_lines[3] == 'clock-color-only'
    assert len(color_lines[4]) == 36
    assert 0 < int(color_lines[2]) - int(float(Path('/proc/uptime').read_text().split()[0])) <= 30
    preview_lines = (guard / 'preview').read_text().splitlines()
    assert preview_lines[0] == 'nubia-ly-preview-v1' and preview_lines[3] == 'lifecycle-only'
    assert len(preview_lines[4]) == 36 and (guard / 'probe').exists()
    run('stop')
    assert not (guard / 'clock').exists()
    assert not (guard / 'preview').exists()
    run('stop')
    assert not (guard / 'probe').exists()
    run('probe')
    run('preview')
    run('clock')
    run('stop', script='service.sh')
    assert not (guard / 'clock').exists()
    assert not (guard / 'preview').exists()
    assert not (guard / 'probe').exists()
    run('probe')
    run('preview')
    run('clock')
    run('stop', script='action.sh')
    assert not (guard / 'clock').exists()
    assert not (guard / 'preview').exists()
    assert not (guard / 'probe').exists()
    run('probe')
    run('preview')
    run('clock')
    run('stop', script='uninstall.sh')
    assert not (guard / 'clock').exists()
    assert not (guard / 'preview').exists()
    assert not (guard / 'probe').exists()
    protected = work / 'unrelated.txt'
    protected.write_text('preserve me')
    (guard / 'probe').symlink_to(protected)
    run('stop')
    assert protected.read_text() == 'preserve me'
    guard.chmod(0o777)
    run('probe', False)
    guard.chmod(0o755)
    guard.rmdir()
    guard.symlink_to(work, target_is_directory=True)
    run('probe', False)
    assert protected.read_text() == 'preserve me'
print('Guard shell tests passed: device/root gates, atomic probe, independent stop/boot/uninstall, unsafe-directory refusal and unrelated-file preservation.')
