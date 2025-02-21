type MenuItem = {
    label: string;
    onClick: () => void;
    className?: string;
};

type MenuListProps = {
    items: MenuItem[];
};

export function MenuList({ items }: MenuListProps) {
    return (
        <div className="mt-6 w-full max-w-md bg-gray-800 shadow-md rounded-xl p-2 border border-gray-700">
            <ul className="text-center divide-y divide-gray-700">
                {items.map(({ label, onClick, className }, index) => (
                    <li
                        key={index}
                        className={`cursor-pointer py-2 text-gray-300 hover:text-white ${className}`}
                        onClick={onClick}>
                        {label}
                    </li>
                ))}
            </ul>
        </div>
    );
}
