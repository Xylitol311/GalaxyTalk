export default async function enableMocking() {
    if (import.meta.env.MODE !== 'development') {
        return;
    }

    const { worker } = await import('./browser.ts');
    return worker.start({
        onUnhandledRequest: 'bypass',
    });
}
