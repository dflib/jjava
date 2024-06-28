import sys
import os
import subprocess


def launch_kernel():
    kernel_path = sys.argv[1]
    connection_file = sys.argv[2]

    args = ['java', '--add-opens', 'jdk.jshell/jdk.jshell=ALL-UNNAMED', '-jar', kernel_path, connection_file]

    jvm_options = os.getenv('JJAVA_JVM_OPTS', '')
    if jvm_options:
        i = 0
        for opt in jvm_options.split(' '):
            i += 1
            args.insert(i, opt)

    subprocess.run(args)


if __name__ == '__main__':
    launch_kernel()
