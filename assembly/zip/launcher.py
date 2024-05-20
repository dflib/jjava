import sys
import os
import subprocess


def launch_kernel():
    kernel_path = sys.argv[1]
    connection_file = sys.argv[2]
    jvm_options = os.getenv('JJ_JVM_OPTS', '')

    subprocess.run(['java',
                    jvm_options,
                    '--add-opens',
                    'jdk.jshell/jdk.jshell=ALL-UNNAMED',
                    '-jar',
                    kernel_path,
                    connection_file])


if __name__ == '__main__':
    launch_kernel()
