package mirai;

import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.GlobalScope;
import net.mamoe.mirai.console.command.CommandManager;
import net.mamoe.mirai.console.pure.MiraiConsolePureLoader;

public class RunMirai {
   
    // ִ�� gradle task: runMiraiConsole ���Զ�����, shadow, ����, ������ pure console.

    public static void main(String[] args) throws InterruptedException {
        // Ĭ���� /test Ŀ¼������

        MiraiConsolePureLoader.load(args[0], args[1]); // ���� console

        // ��ֹ���߳��˳�
        BuildersKt.runBlocking(GlobalScope.INSTANCE.getCoroutineContext(), (coroutineScope, continuation) -> CommandManager.INSTANCE.join(continuation));
    }
}